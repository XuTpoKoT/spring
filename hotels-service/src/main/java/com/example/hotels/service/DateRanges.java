package com.example.hotels.service;

import java.time.LocalDate;

import com.example.hotels.exception.ApiException;

final class DateRanges {

    private DateRanges() {
    }

    static void validateRequired(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw ApiException.badRequest("startDate and endDate are required");
        }
        validateOrder(startDate, endDate);
    }

    static void validateOptional(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw ApiException.badRequest("startDate and endDate must be provided together");
        }
        if (startDate != null) {
            validateOrder(startDate, endDate);
        }
    }

    private static void validateOrder(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw ApiException.badRequest("endDate must be after startDate");
        }
    }
}
