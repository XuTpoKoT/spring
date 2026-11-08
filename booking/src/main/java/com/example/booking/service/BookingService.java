package com.example.booking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.booking.client.HotelAvailabilityClient;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.User;
import com.example.booking.exception.ApiException;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.model.AdminBookingCreateRequestDto;
import com.example.booking.model.BookingCreateRequestDto;
import com.example.booking.model.BookingDto;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookings;
    private final UserRepository users;
    private final BookingTxService bookingTxService;
    private final HotelAvailabilityClient hotelClient;
    private final BookingMapper bookingMapper;

    public BookingDto create(Long userId, BookingCreateRequestDto request,
                             String authorization, String requestId) {
        validateDates(request.getStartDate(), request.getEndDate());

        Optional<Booking> existing = findExisting(requestId);
        if (existing.isPresent()) {
            Booking booking = existing.get();
            if (booking.getStatus() == BookingStatus.PENDING) {
                log.info("Resuming PENDING booking {} for requestId={}", booking.getId(), requestId);
                return confirmOrCompensate(booking, authorization, requestId);
            }
            return bookingMapper.toDto(booking);
        }

        Long roomId;
        if (Boolean.TRUE.equals(request.getAutoSelect())) {
            roomId = hotelClient.recommendRoom(authorization, correlation(requestId, null, "recommend"));
        } else {
            if (request.getRoomId() == null) {
                throw ApiException.badRequest("roomId is required when autoSelect is false");
            }
            roomId = request.getRoomId();
        }

        return doCreate(userId, roomId, request.getStartDate(), request.getEndDate(), authorization, requestId);
    }

    public BookingDto createForUser(AdminBookingCreateRequestDto request,
                                    String authorization, String requestId) {
        validateDates(request.getStartDate(), request.getEndDate());

        User targetUser = users.findByUsername(request.getUsername())
                .orElseThrow(() -> ApiException.notFound("User not found: " + request.getUsername()));

        BookingCreateRequestDto mapped = new BookingCreateRequestDto();
        mapped.setAutoSelect(request.getAutoSelect());
        mapped.setRoomId(request.getRoomId());
        mapped.setStartDate(request.getStartDate());
        mapped.setEndDate(request.getEndDate());

        return create(targetUser.getId(), mapped, authorization, requestId);
    }

    @Transactional(readOnly = true)
    public List<BookingDto> listForUser(Long userId) {
        return bookings.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingDto get(Long bookingId, Long userId, boolean admin) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + bookingId));

        if (!admin && !booking.getUserId().equals(userId)) {
            throw ApiException.forbidden("Booking belongs to another user");
        }

        return bookingMapper.toDto(booking);
    }

    public void cancel(Long bookingId, Long userId, boolean admin,
                       String authorization, String requestId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + bookingId));

        if (!admin && !booking.getUserId().equals(userId)) {
            throw ApiException.forbidden("Booking belongs to another user");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.conflict("Booking is already cancelled");
        }

        Long roomId = booking.getRoomId();
        bookingTxService.markCancelled(bookingId);
        log.info("Booking {} moved to CANCELLED (requestId={})", bookingId, requestId);

        try {
            hotelClient.releaseRoom(authorization, roomId, bookingId,
                    correlation(requestId, bookingId, "release"));
        } catch (RuntimeException ex) {
            log.error("Booking {} compensation release failed: {}", bookingId, ex.getMessage(), ex);
        }
    }

    private BookingDto doCreate(Long userId, Long roomId, LocalDate startDate, LocalDate endDate,
                                String authorization, String requestId) {
        Booking pending;
        try {
            pending = bookingTxService.createPending(userId, roomId, startDate, endDate, requestId);
        } catch (DataIntegrityViolationException ex) {
            Optional<Booking> existing = findExisting(requestId);
            if (existing.isPresent()) {
                Booking booking = existing.get();
                if (booking.getStatus() == BookingStatus.PENDING) {
                    return confirmOrCompensate(booking, authorization, requestId);
                }
                return bookingMapper.toDto(booking);
            }
            throw ApiException.conflict("Booking request conflicts with an existing record");
        }

        log.info("Booking {} created in PENDING (userId={}, roomId={}, requestId={})",
                pending.getId(), userId, roomId, requestId);

        return confirmOrCompensate(pending, authorization, requestId);
    }

    private BookingDto confirmOrCompensate(Booking pending, String authorization, String requestId) {
        Long bookingId = pending.getId();
        Long roomId = pending.getRoomId();
        LocalDate startDate = pending.getStartDate();
        LocalDate endDate = pending.getEndDate();

        try {
            hotelClient.confirmAvailability(authorization, roomId, bookingId, startDate, endDate,
                    correlation(requestId, bookingId, "confirm"));
            bookingTxService.markConfirmed(bookingId);
            log.info("Booking {} moved to CONFIRMED (requestId={})", bookingId, requestId);
        } catch (RuntimeException ex) {
            log.warn("Booking {} availability confirmation failed: {}", bookingId, ex.getMessage());
            bookingTxService.markCancelled(bookingId);
            log.info("Booking {} moved to CANCELLED after failed confirmation (requestId={})",
                    bookingId, requestId);

            try {
                hotelClient.releaseRoom(authorization, roomId, bookingId,
                        correlation(requestId, bookingId, "release"));
            } catch (RuntimeException releaseEx) {
                log.error("Booking {} compensation release failed: {}",
                        bookingId, releaseEx.getMessage(), releaseEx);
            }

            if (ex instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.serviceUnavailable("Failed to confirm booking availability");
        }

        return bookingMapper.toDto(bookingTxService.get(bookingId));
    }

    private Optional<Booking> findExisting(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return Optional.empty();
        }
        return bookings.findByRequestId(requestId);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw ApiException.badRequest("startDate and endDate are required");
        }
        if (!endDate.isAfter(startDate)) {
            throw ApiException.badRequest("endDate must be after startDate");
        }
    }

    private String correlation(String requestId, Long bookingId, String stage) {
        String base;
        if (requestId != null && !requestId.isBlank()) {
            base = requestId;
        } else if (bookingId != null) {
            base = "booking-" + bookingId;
        } else {
            base = "request";
        }

        String value = base + ":" + stage;
        return value.length() <= 64 ? value : value.substring(0, 64);
    }
}
