package com.example.booking.controller;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.booking.model.AdminBookingCreateRequestDto;
import com.example.booking.model.BookingDto;
import com.example.booking.security.CurrentUser;
import com.example.booking.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;

    @PostMapping("/admin/bookings")
    public ResponseEntity<BookingDto> create(
            @Valid @RequestBody AdminBookingCreateRequestDto request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        BookingDto created = bookingService.createForUser(request, authorization, requestId);
        return ResponseEntity.created(URI.create("/booking/" + created.getId())).body(created);
    }

    @DeleteMapping("/admin/bookings/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        bookingService.cancel(id, CurrentUser.id(), true, authorization, requestId);
    }
}
