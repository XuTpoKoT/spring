package com.example.hotels.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotels.model.AvailabilityConfirmationDto;
import com.example.hotels.model.AvailabilityRequestDto;
import com.example.hotels.model.ReleaseRequestDto;
import com.example.hotels.service.RoomAvailabilityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InternalRoomController {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RoomAvailabilityService availabilityService;

    @PostMapping("/rooms/{id}/confirm-availability")
    public AvailabilityConfirmationDto confirmAvailability(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequestDto request,
            @RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
        return availabilityService.confirm(id, request, requestId);
    }

    @PostMapping("/rooms/{id}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(
            @PathVariable Long id,
            @Valid @RequestBody ReleaseRequestDto request,
            @RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId) {
        availabilityService.release(id, request, requestId);
    }
}
