package com.example.hotels.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.hotels.config.RoomProperties;
import com.example.hotels.entity.Room;
import com.example.hotels.entity.RoomLock;
import com.example.hotels.exception.ApiException;
import com.example.hotels.model.AvailabilityConfirmationDto;
import com.example.hotels.model.AvailabilityRequestDto;
import com.example.hotels.model.ReleaseRequestDto;
import com.example.hotels.repository.RoomLockRepository;
import com.example.hotels.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityServiceTest {

    private static final Long ROOM_ID = 101L;
    private static final Long BOOKING_ID = 1L;

    @Mock
    private RoomRepository rooms;

    @Mock
    private RoomLockRepository locks;

    private RoomAvailabilityService service;

    private final LocalDate startDate = LocalDate.now().plusDays(1);
    private final LocalDate endDate = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        service = new RoomAvailabilityService(rooms, locks, new RoomProperties(Duration.ofMinutes(10)));
    }

    @Test
    void createsLockAndIncrementsTimesBooked() {
        Room room = room(3);
        when(rooms.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(locks.findByRequestId("request-1")).thenReturn(Optional.empty());
        when(locks.findByRoomIdAndBookingId(ROOM_ID, BOOKING_ID)).thenReturn(Optional.empty());
        when(locks.countActiveOverlaps(eq(ROOM_ID), eq(startDate), eq(endDate), any())).thenReturn(0L);

        AvailabilityConfirmationDto result = service.confirm(
                ROOM_ID, request(), "request-1");

        assertThat(result.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(result.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(result.getConfirmed()).isTrue();
        assertThat(room.getTimesBooked()).isEqualTo(4);
        verify(locks).save(any(RoomLock.class));
        verify(rooms).save(room);
    }

    @Test
    void rejectsOverlappingActiveLock() {
        Room room = room(3);
        when(rooms.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(locks.findByRequestId("request-1")).thenReturn(Optional.empty());
        when(locks.findByRoomIdAndBookingId(ROOM_ID, BOOKING_ID)).thenReturn(Optional.empty());
        when(locks.countActiveOverlaps(eq(ROOM_ID), eq(startDate), eq(endDate), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.confirm(ROOM_ID, request(), "request-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not available");

        verify(locks, never()).save(any(RoomLock.class));
        assertThat(room.getTimesBooked()).isEqualTo(3);
    }

    @Test
    void returnsExistingActiveLockIdempotently() {
        Room room = room(3);
        RoomLock lock = lock(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        when(rooms.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(locks.findByRequestId("request-1")).thenReturn(Optional.of(lock));

        AvailabilityConfirmationDto result = service.confirm(ROOM_ID, request(), "request-1");

        assertThat(result.getLockExpiresAt()).isEqualTo(lock.getExpiresAt());
        assertThat(room.getTimesBooked()).isEqualTo(3);
        verify(locks, never()).save(any(RoomLock.class));
    }

    @Test
    void renewsExpiredLockWithoutDoubleCounting() {
        Room room = room(3);
        RoomLock expired = lock(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(rooms.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(locks.findByRequestId("request-1")).thenReturn(Optional.of(expired));
        when(locks.countActiveOverlapsExcluding(
                eq(ROOM_ID), eq(startDate), eq(endDate), any(), eq(expired.getId()))).thenReturn(0L);

        AvailabilityConfirmationDto result = service.confirm(ROOM_ID, request(), "request-1");

        assertThat(result.getLockExpiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(room.getTimesBooked()).isEqualTo(3);
        verify(locks).save(expired);
        verify(rooms, never()).save(room);
    }

    @Test
    void releaseDeletesLockIdempotently() {
        RoomLock lock = lock(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        when(rooms.existsById(ROOM_ID)).thenReturn(true);
        when(locks.findByRoomIdAndBookingId(ROOM_ID, BOOKING_ID)).thenReturn(Optional.of(lock));

        service.release(ROOM_ID, new ReleaseRequestDto(BOOKING_ID), "release-1");

        verify(locks).delete(lock);
    }

    @Test
    void releaseOfMissingLockIsSuccessful() {
        when(rooms.existsById(ROOM_ID)).thenReturn(true);
        when(locks.findByRoomIdAndBookingId(ROOM_ID, BOOKING_ID)).thenReturn(Optional.empty());

        service.release(ROOM_ID, new ReleaseRequestDto(BOOKING_ID), "release-1");

        verify(locks, never()).delete(any());
    }

    @Test
    void rejectInvalidDateRange() {
        assertThatThrownBy(() -> service.confirm(ROOM_ID,
                new AvailabilityRequestDto(BOOKING_ID, endDate, startDate), null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("endDate");
    }

    private AvailabilityRequestDto request() {
        return new AvailabilityRequestDto(BOOKING_ID, startDate, endDate);
    }

    private Room room(int timesBooked) {
        Room room = new Room();
        room.setId(ROOM_ID);
        room.setHotelId(1L);
        room.setNumber("101");
        room.setAvailable(true);
        room.setTimesBooked(timesBooked);
        return room;
    }

    private RoomLock lock(OffsetDateTime expiresAt) {
        RoomLock lock = new RoomLock();
        lock.setId(500L);
        lock.setRoomId(ROOM_ID);
        lock.setBookingId(BOOKING_ID);
        lock.setStartDate(startDate);
        lock.setEndDate(endDate);
        lock.setExpiresAt(expiresAt);
        lock.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        lock.setRequestId("request-1");
        return lock;
    }
}
