package com.example.booking.config;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataSeeder {

    private final UserRepository users;
    private final BookingRepository bookings;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seed() {
        if (users.count() > 0) {
            log.info("Demo data already present, seeding skipped");
            return;
        }

        User admin = createUser("admin", "admin12345", Role.ADMIN);
        User user = createUser("user", "user12345", Role.USER);
        users.saveAll(List.of(admin, user));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Booking> demoBookings = List.of(
                createBooking(user.getId(), 101L, today.plusDays(1), today.plusDays(3),
                        BookingStatus.CONFIRMED, "seed-confirmed-1", 1),
                createBooking(user.getId(), 102L, today.plusDays(5), today.plusDays(7),
                        BookingStatus.PENDING, "seed-pending-1", 0),
                createBooking(user.getId(), 103L, today.minusDays(5), today.minusDays(3),
                        BookingStatus.CANCELLED, "seed-cancelled-1", 6),
                createBooking(admin.getId(), 201L, today.plusDays(10), today.plusDays(12),
                        BookingStatus.CONFIRMED, "seed-admin-1", 2)
        );
        bookings.saveAll(demoBookings);

        log.info("Demo data created. Users: admin/admin12345 (ADMIN), user/user12345 (USER)");
        demoBookings.forEach(booking -> log.info(
                "Seeded booking id={} userId={} roomId={} status={} requestId={}",
                booking.getId(), booking.getUserId(), booking.getRoomId(),
                booking.getStatus(), booking.getRequestId()));
    }

    private User createUser(String username, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return user;
    }

    private Booking createBooking(Long userId, Long roomId, LocalDate startDate, LocalDate endDate,
                                  BookingStatus status, String requestId, long daysAgo) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setRoomId(roomId);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(status);
        booking.setRequestId(requestId);
        booking.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysAgo));
        return booking;
    }
}
