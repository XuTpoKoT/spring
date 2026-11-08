package com.example.booking.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "hotels-service",
        url = "${app.hotels.base-url}",
        configuration = HotelsFeignConfiguration.class
)
public interface HotelsFeignClient {

    String REQUEST_ID_HEADER = "X-Request-Id";

    @GetMapping("/rooms/recommend")
    List<HotelRoom> recommendRooms(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId);

    @PostMapping("/rooms/{id}/confirm-availability")
    void confirmAvailability(
            @PathVariable("id") Long roomId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId,
            @RequestBody AvailabilityRequest request);

    @PostMapping("/rooms/{id}/release")
    void releaseRoom(
            @PathVariable("id") Long roomId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = REQUEST_ID_HEADER, required = false) String requestId,
            @RequestBody ReleaseRequest request);
}
