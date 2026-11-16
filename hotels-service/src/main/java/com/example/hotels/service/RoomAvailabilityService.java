package com.example.hotels.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.hotels.config.RoomProperties;
import com.example.hotels.entity.Room;
import com.example.hotels.entity.RoomLock;
import com.example.hotels.exception.ApiException;
import com.example.hotels.model.AvailabilityConfirmationDto;
import com.example.hotels.model.AvailabilityRequestDto;
import com.example.hotels.model.ReleaseRequestDto;
import com.example.hotels.repository.RoomLockRepository;
import com.example.hotels.repository.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomAvailabilityService {

    private final RoomRepository rooms;
    private final RoomLockRepository locks;
    private final RoomProperties properties;

    @Transactional
    public AvailabilityConfirmationDto confirm(Long roomId, AvailabilityRequestDto request,
                                               String requestId) {
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        DateRanges.validateRequired(startDate, endDate);

        Room room = rooms.findByIdForUpdate(roomId)
                .orElseThrow(() -> ApiException.notFound("Room not found: " + roomId));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String normalizedRequestId = normalizeRequestId(requestId);

        RoomLock existing = findExisting(roomId, request.getBookingId(), normalizedRequestId, now);
        if (existing != null) {
            return handleExisting(existing, roomId, request, startDate, endDate, now, normalizedRequestId);
        }

        long overlaps = locks.countActiveOverlaps(roomId, startDate, endDate, now);
        if (overlaps > 0) {
            throw unavailable(roomId);
        }

        RoomLock lock = new RoomLock();
        lock.setRoomId(roomId);
        lock.setBookingId(request.getBookingId());
        lock.setStartDate(startDate);
        lock.setEndDate(endDate);
        lock.setExpiresAt(now.plus(properties.lockTtl()));
        lock.setCreatedAt(now);
        lock.setRequestId(normalizedRequestId);
        locks.save(lock);

        room.setTimesBooked(room.getTimesBooked() + 1);
        rooms.save(room);

        log.info("Availability lock created: roomId={}, bookingId={}, {}..{}, expiresAt={}, requestId={}",
                roomId, request.getBookingId(), startDate, endDate, lock.getExpiresAt(), normalizedRequestId);

        return toConfirmation(lock);
    }

    @Transactional
    public void release(Long roomId, ReleaseRequestDto request, String requestId) {
        if (!rooms.existsById(roomId)) {
            throw ApiException.notFound("Room not found: " + roomId);
        }

        locks.findByRoomIdAndBookingId(roomId, request.getBookingId())
                .ifPresent(lock -> {
                    locks.delete(lock);
                    log.info("Availability lock released: roomId={}, bookingId={}, requestId={}",
                            roomId, request.getBookingId(), requestId);
                });
    }

    private RoomLock findExisting(Long roomId, Long bookingId, String requestId,
                                  OffsetDateTime now) {
        if (requestId != null) {
            Optional<RoomLock> byRequestId = locks.findByRequestId(requestId);
            if (byRequestId.isPresent()) {
                RoomLock lock = byRequestId.get();
                if (!lock.getRoomId().equals(roomId) || !lock.getBookingId().equals(bookingId)) {
                    throw ApiException.conflict(
                            "X-Request-Id has already been used for another availability request");
                }
                return lock;
            }
        }
        return locks.findByRoomIdAndBookingId(roomId, bookingId).orElse(null);
    }

    private AvailabilityConfirmationDto handleExisting(RoomLock existing, Long roomId,
                                                       AvailabilityRequestDto request,
                                                       LocalDate startDate, LocalDate endDate,
                                                       OffsetDateTime now, String requestId) {
        if (existing.getExpiresAt().isAfter(now)) {
            return toConfirmation(existing);
        }

        long overlaps = locks.countActiveOverlapsExcluding(
                roomId, startDate, endDate, now, existing.getId());
        if (overlaps > 0) {
            throw unavailable(roomId);
        }

        existing.setStartDate(startDate);
        existing.setEndDate(endDate);
        existing.setExpiresAt(now.plus(properties.lockTtl()));
        if (existing.getRequestId() == null && requestId != null) {
            existing.setRequestId(requestId);
        }
        locks.save(existing);

        log.info("Availability lock renewed: roomId={}, bookingId={}, {}..{}, expiresAt={}",
                roomId, request.getBookingId(), startDate, endDate, existing.getExpiresAt());

        return toConfirmation(existing);
    }

    private AvailabilityConfirmationDto toConfirmation(RoomLock lock) {
        return new AvailabilityConfirmationDto(
                lock.getRoomId(),
                lock.getBookingId(),
                true,
                lock.getStartDate(),
                lock.getEndDate(),
                lock.getExpiresAt()
        );
    }

    private ApiException unavailable(Long roomId) {
        return ApiException.conflict("Room " + roomId + " is not available for the requested period");
    }

    private String normalizeRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        String value = requestId.trim();
        if (value.length() > 64) {
            throw ApiException.badRequest("X-Request-Id must not be longer than 64 characters");
        }
        return value;
    }
}
