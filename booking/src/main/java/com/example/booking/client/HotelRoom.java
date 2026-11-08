package com.example.booking.client;

public record HotelRoom(
        Long id,
        Long hotelId,
        String number,
        Boolean available,
        Integer timesBooked
) {
}
