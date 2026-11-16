package com.example.hotels.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.hotels.entity.RoomLock;

public interface RoomLockRepository extends JpaRepository<RoomLock, Long> {

    Optional<RoomLock> findByRoomIdAndBookingId(Long roomId, Long bookingId);

    Optional<RoomLock> findByRequestId(String requestId);

    @Query("""
            select count(l)
            from RoomLock l
            where l.roomId = :roomId
              and l.expiresAt > :now
              and l.startDate < :endDate
              and l.endDate > :startDate
            """)
    long countActiveOverlaps(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("now") OffsetDateTime now
    );

    @Query("""
            select count(l)
            from RoomLock l
            where l.roomId = :roomId
              and l.id <> :excludeId
              and l.expiresAt > :now
              and l.startDate < :endDate
              and l.endDate > :startDate
            """)
    long countActiveOverlapsExcluding(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("now") OffsetDateTime now,
            @Param("excludeId") Long excludeId
    );
}
