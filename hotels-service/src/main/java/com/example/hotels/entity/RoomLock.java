package com.example.hotels.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "room_locks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_locks_booking", columnNames = {"room_id", "booking_id"}),
                @UniqueConstraint(name = "uk_room_locks_request", columnNames = "request_id")
        },
        indexes = @Index(
                name = "idx_room_locks_room_dates",
                columnList = "room_id, start_date, end_date"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class RoomLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "request_id", length = 64)
    private String requestId;
}
