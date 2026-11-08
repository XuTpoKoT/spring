package com.example.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.booking.client.HotelAvailabilityClient;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.exception.ApiException;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.model.BookingCreateRequestDto;
import com.example.booking.model.BookingDto;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long ROOM_ID = 10L;
    private static final Long BOOKING_ID = 100L;

    @Mock
    private BookingRepository bookings;

    @Mock
    private UserRepository users;

    @Mock
    private BookingTxService bookingTxService;

    @Mock
    private HotelAvailabilityClient hotelClient;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private final LocalDate startDate = LocalDate.now().plusDays(1);
    private final LocalDate endDate = LocalDate.now().plusDays(3);

    @Test
    void createsBookingAndConfirmsIt() {
        BookingCreateRequestDto request = requestWithRoom();
        Booking pending = booking(BookingStatus.PENDING);
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        BookingDto expected = dto(BookingDto.StatusEnum.CONFIRMED);

        when(bookingTxService.createPending(USER_ID, ROOM_ID, startDate, endDate, null)).thenReturn(pending);
        when(bookingTxService.get(BOOKING_ID)).thenReturn(confirmed);
        when(bookingMapper.toDto(confirmed)).thenReturn(expected);

        BookingDto result = bookingService.create(USER_ID, request, "Bearer token", null);

        assertThat(result).isSameAs(expected);
        verify(hotelClient).confirmAvailability(eq("Bearer token"), eq(ROOM_ID), eq(BOOKING_ID),
                eq(startDate), eq(endDate), anyString());
        verify(bookingTxService).markConfirmed(BOOKING_ID);
        verify(hotelClient, never()).releaseRoom(any(), any(), any(), any());
    }

    @Test
    void cancelsBookingAndReleasesRoomWhenConfirmationFails() {
        BookingCreateRequestDto request = requestWithRoom();
        Booking pending = booking(BookingStatus.PENDING);

        when(bookingTxService.createPending(USER_ID, ROOM_ID, startDate, endDate, null)).thenReturn(pending);
        doThrow(ApiException.conflict("Room is not available"))
                .when(hotelClient).confirmAvailability(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> bookingService.create(USER_ID, request, "Bearer token", null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not available");

        verify(bookingTxService).markCancelled(BOOKING_ID);
        verify(hotelClient).releaseRoom(eq("Bearer token"), eq(ROOM_ID), eq(BOOKING_ID), anyString());
        verify(bookingTxService, never()).markConfirmed(any());
    }

    @Test
    void returnsExistingBookingForRepeatedRequestId() {
        Booking existing = booking(BookingStatus.CONFIRMED);
        BookingDto expected = dto(BookingDto.StatusEnum.CONFIRMED);

        when(bookings.findByRequestId("request-1")).thenReturn(Optional.of(existing));
        when(bookingMapper.toDto(existing)).thenReturn(expected);

        BookingDto result = bookingService.create(USER_ID, requestWithRoom(), "Bearer token", "request-1");

        assertThat(result).isSameAs(expected);
        verify(bookingTxService, never()).createPending(any(), any(), any(), any(), any());
        verify(hotelClient, never()).confirmAvailability(any(), any(), any(), any(), any(), any());
    }

    @Test
    void autoSelectAsksHotelsForRecommendedRoom() {
        BookingCreateRequestDto request = new BookingCreateRequestDto(true, startDate, endDate);
        Booking pending = booking(BookingStatus.PENDING);
        Booking confirmed = booking(BookingStatus.CONFIRMED);
        BookingDto expected = dto(BookingDto.StatusEnum.CONFIRMED);

        when(hotelClient.recommendRoom(null, "request-2:recommend")).thenReturn(ROOM_ID);
        when(bookingTxService.createPending(USER_ID, ROOM_ID, startDate, endDate, "request-2")).thenReturn(pending);
        when(bookingTxService.get(BOOKING_ID)).thenReturn(confirmed);
        when(bookingMapper.toDto(confirmed)).thenReturn(expected);

        BookingDto result = bookingService.create(USER_ID, request, null, "request-2");

        assertThat(result).isSameAs(expected);
        verify(hotelClient).recommendRoom(null, "request-2:recommend");
    }

    private BookingCreateRequestDto requestWithRoom() {
        BookingCreateRequestDto request = new BookingCreateRequestDto(false, startDate, endDate);
        request.setRoomId(ROOM_ID);
        return request;
    }

    private Booking booking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setUserId(USER_ID);
        booking.setRoomId(ROOM_ID);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(status);
        booking.setCreatedAt(OffsetDateTime.now());
        return booking;
    }

    private BookingDto dto(BookingDto.StatusEnum status) {
        return new BookingDto(BOOKING_ID, USER_ID, ROOM_ID, startDate, endDate, status, OffsetDateTime.now());
    }
}
