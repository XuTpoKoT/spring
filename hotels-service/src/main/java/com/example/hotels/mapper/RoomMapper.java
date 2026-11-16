package com.example.hotels.mapper;

import org.mapstruct.Mapper;

import com.example.hotels.entity.Room;
import com.example.hotels.model.RoomDto;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    RoomDto toDto(Room room);
}
