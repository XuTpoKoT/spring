package com.example.hotels.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotels.model.RoomDto;
import com.example.hotels.model.RoomInputDto;
import com.example.hotels.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/rooms")
    public List<RoomDto> list(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return roomService.listFree(hotelId, startDate, endDate);
    }

    @GetMapping("/rooms/recommend")
    public List<RoomDto> recommend(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return roomService.recommend(hotelId, startDate, endDate);
    }

    @PostMapping("/admin/rooms")
    public ResponseEntity<RoomDto> create(@Valid @RequestBody RoomInputDto request) {
        RoomDto created = roomService.create(request);
        return ResponseEntity.created(URI.create("/rooms/" + created.getId())).body(created);
    }
}
