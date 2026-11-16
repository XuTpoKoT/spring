package com.example.hotels.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotels.repository.HotelRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelDemoDataSeeder {

    private final HotelRepository hotels;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void seed() {
        if (hotels.count() > 0) {
            log.info("Demo data already present, seeding skipped");
            return;
        }

        jdbcTemplate.update("insert into hotels (id, name, address) values (?, ?, ?)",
                1L, "Grand Hotel", "Москва, Тверская ул., 1");
        jdbcTemplate.update("insert into hotels (id, name, address) values (?, ?, ?)",
                2L, "Seaside Resort", "Сочи, Морская ул., 10");

        insertRoom(101L, 1L, "101", true, 3);
        insertRoom(102L, 1L, "102", true, 1);
        insertRoom(103L, 1L, "103", true, 7);
        insertRoom(104L, 1L, "104", false, 0);
        insertRoom(201L, 2L, "201", true, 2);
        insertRoom(202L, 2L, "202", true, 0);

        jdbcTemplate.execute("alter table hotels alter column id restart with 1000");
        jdbcTemplate.execute("alter table rooms alter column id restart with 1000");

        log.info("Demo data created: hotels=2, rooms=6");
        log.info("Hotels: 1=Grand Hotel, 2=Seaside Resort");
        log.info("Rooms: 101,102,103,104 (hotel 1), 201,202 (hotel 2); room 104 is unavailable");
    }

    private void insertRoom(Long id, Long hotelId, String number, boolean available, int timesBooked) {
        jdbcTemplate.update("""
                insert into rooms (id, hotel_id, number, available, times_booked)
                values (?, ?, ?, ?, ?)
                """, id, hotelId, number, available, timesBooked);
    }
}
