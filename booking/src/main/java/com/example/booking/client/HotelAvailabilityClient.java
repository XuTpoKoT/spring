package com.example.booking.client;

import java.time.LocalDate;

public interface HotelAvailabilityClient {

    Long recommendRoom(String authorization, String requestId);

    void confirmAvailability(String authorization, Long roomId, Long bookingId,
                             LocalDate startDate, LocalDate endDate, String requestId);

    void releaseRoom(String authorization, Long roomId, Long bookingId, String requestId);
}
