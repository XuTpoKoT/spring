package com.example.booking.client;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.booking.exception.ApiException;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignHotelAvailabilityClient implements HotelAvailabilityClient {

    private final HotelsFeignClient hotelsFeignClient;

    @Override
    public Long recommendRoom(String authorization, String requestId) {
        try {
            List<HotelRoom> rooms = hotelsFeignClient.recommendRooms(authorization, requestId);
            if (rooms == null || rooms.isEmpty()) {
                throw ApiException.conflict("No available room for the requested period");
            }
            return rooms.getFirst().id();
        } catch (FeignException ex) {
            throw mapError("recommend-room", ex);
        }
    }

    @Override
    public void confirmAvailability(String authorization, Long roomId, Long bookingId,
                                    LocalDate startDate, LocalDate endDate, String requestId) {
        try {
            hotelsFeignClient.confirmAvailability(roomId, authorization, requestId,
                    new AvailabilityRequest(bookingId, startDate, endDate));
        } catch (FeignException ex) {
            throw mapError("confirm-availability", ex);
        }
    }

    @Override
    public void releaseRoom(String authorization, Long roomId, Long bookingId, String requestId) {
        try {
            hotelsFeignClient.releaseRoom(roomId, authorization, requestId,
                    new ReleaseRequest(bookingId));
        } catch (FeignException ex) {
            throw mapError("release-room", ex);
        }
    }

    private ApiException mapError(String operation, FeignException exception) {
        int status = exception.status();
        log.warn("hotels-service {} failed with status {}", operation, status);

        return switch (status) {
            case 400 -> ApiException.badRequest("hotels-service rejected request: " + exception.contentUTF8());
            case 404 -> ApiException.notFound("Room or hotel resource not found");
            case 409 -> ApiException.conflict("Room is not available for the requested period");
            case 401, 403 -> ApiException.serviceUnavailable(
                    "hotels-service rejected the forwarded credentials: status " + status);
            case 429 -> ApiException.serviceUnavailable(
                    "hotels-service rate limit exceeded after retries");
            default -> ApiException.serviceUnavailable(
                    "hotels-service " + operation + " failed after retries: status " + status);
        };
    }
}
