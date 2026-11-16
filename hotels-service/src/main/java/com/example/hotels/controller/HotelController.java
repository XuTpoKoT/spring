package com.example.hotels.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotels.model.HotelDto;
import com.example.hotels.model.HotelInputDto;
import com.example.hotels.service.HotelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping("/hotels")
    public List<HotelDto> list() {
        return hotelService.list();
    }

    @PostMapping("/admin/hotels")
    public ResponseEntity<HotelDto> create(@Valid @RequestBody HotelInputDto request) {
        HotelDto created = hotelService.create(request);
        return ResponseEntity.created(URI.create("/hotels/" + created.getId())).body(created);
    }
}
