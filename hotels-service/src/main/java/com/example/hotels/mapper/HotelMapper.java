package com.example.hotels.mapper;

import org.mapstruct.Mapper;

import com.example.hotels.entity.Hotel;
import com.example.hotels.model.HotelDto;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    HotelDto toDto(Hotel hotel);
}
