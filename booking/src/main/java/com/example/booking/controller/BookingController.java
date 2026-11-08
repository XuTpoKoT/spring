package com.example.booking.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.booking.model.BookingCreateRequestDto;
import com.example.booking.model.BookingDto;
import com.example.booking.security.CurrentUser;
import com.example.booking.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    public ResponseEntity<BookingDto> create(
            @Valid @RequestBody BookingCreateRequestDto request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        BookingDto created = bookingService.create(CurrentUser.id(), request, authorization, requestId);
        return ResponseEntity.created(URI.create("/booking/" + created.getId())).body(created);
    }

    @GetMapping("/bookings")
    public List<BookingDto> list() {
        return bookingService.listForUser(CurrentUser.id());
    }

    @GetMapping("/booking/{id}")
    public BookingDto get(@PathVariable Long id) {
        return bookingService.get(id, CurrentUser.id(), CurrentUser.isAdmin());
    }

    @DeleteMapping("/booking/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void cancel(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        bookingService.cancel(id, CurrentUser.id(), CurrentUser.isAdmin(), authorization, requestId);
    }
}
