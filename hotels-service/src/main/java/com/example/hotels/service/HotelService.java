package com.example.hotels.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotels.entity.Hotel;
import com.example.hotels.exception.ApiException;
import com.example.hotels.mapper.HotelMapper;
import com.example.hotels.model.HotelDto;
import com.example.hotels.model.HotelInputDto;
import com.example.hotels.repository.HotelRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotels;
    private final HotelMapper hotelMapper;

    @Transactional(readOnly = true)
    public List<HotelDto> list() {
        return hotels.findAllByOrderByIdAsc().stream()
                .map(hotelMapper::toDto)
                .toList();
    }

    @Transactional
    public HotelDto create(HotelInputDto input) {
        String name = input.getName().trim();
        String address = input.getAddress().trim();

        if (hotels.existsByName(name)) {
            throw ApiException.conflict("Hotel with name '" + name + "' already exists");
        }

        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotels.save(hotel);

        return hotelMapper.toDto(hotel);
    }
}
