package com.example.booking.client;

import java.time.LocalDate;

public record AvailabilityRequest(Long bookingId, LocalDate startDate, LocalDate endDate) {
}
