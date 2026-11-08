package com.example.booking.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.exception.ApiException;
import com.example.booking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingTxService {

    private final BookingRepository bookings;

    @Transactional
    public Booking createPending(Long userId, Long roomId, LocalDate startDate, LocalDate endDate,
                                 String requestId) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setRoomId(roomId);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        booking.setRequestId(requestId);
        return bookings.saveAndFlush(booking);
    }

    @Transactional
    public void markConfirmed(Long bookingId) {
        Booking booking = get(bookingId);
        booking.confirm();
    }

    @Transactional
    public void markCancelled(Long bookingId) {
        Booking booking = get(bookingId);
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            booking.cancel();
        }
    }

    @Transactional(readOnly = true)
    public Booking get(Long bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + bookingId));
    }
}
