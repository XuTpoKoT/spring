package com.example.hotels.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotels.entity.Room;
import com.example.hotels.exception.ApiException;
import com.example.hotels.mapper.RoomMapper;
import com.example.hotels.model.RoomDto;
import com.example.hotels.model.RoomInputDto;
import com.example.hotels.repository.HotelRepository;
import com.example.hotels.repository.RoomLockRepository;
import com.example.hotels.repository.RoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository rooms;
    private final RoomLockRepository locks;
    private final HotelRepository hotels;
    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public List<RoomDto> listFree(Long hotelId, LocalDate startDate, LocalDate endDate) {
        DateRanges.validateOptional(startDate, endDate);
        return filter(rooms.findByAvailableTrueOrderByIdAsc(), hotelId, startDate, endDate).stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomDto> recommend(Long hotelId, LocalDate startDate, LocalDate endDate) {
        DateRanges.validateOptional(startDate, endDate);
        return filter(rooms.findByAvailableTrueOrderByIdAsc(), hotelId, startDate, endDate).stream()
                .sorted(Comparator.comparingInt(Room::getTimesBooked).thenComparing(Room::getId))
                .map(roomMapper::toDto)
                .toList();
    }

    @Transactional
    public RoomDto create(RoomInputDto input) {
        Long hotelId = input.getHotelId();
        if (!hotels.existsById(hotelId)) {
            throw ApiException.notFound("Hotel not found: " + hotelId);
        }

        String number = input.getNumber().trim();
        if (rooms.existsByHotelIdAndNumber(hotelId, number)) {
            throw ApiException.conflict(
                    "Room with number '" + number + "' already exists in hotel " + hotelId);
        }

        Room room = new Room();
        room.setHotelId(hotelId);
        room.setNumber(number);
        room.setAvailable(input.getAvailable() == null || input.getAvailable());
        room.setTimesBooked(0);
        rooms.save(room);

        return roomMapper.toDto(room);
    }

    private List<Room> filter(List<Room> rooms, Long hotelId, LocalDate startDate, LocalDate endDate) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return rooms.stream()
                .filter(room -> hotelId == null || hotelId.equals(room.getHotelId()))
                .filter(room -> startDate == null
                        || locks.countActiveOverlaps(room.getId(), startDate, endDate, now) == 0)
                .toList();
    }
}
