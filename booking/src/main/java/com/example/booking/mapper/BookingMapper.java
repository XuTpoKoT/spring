package com.example.booking.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.model.BookingDto;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "status", source = "status", qualifiedByName = "toStatus")
    BookingDto toDto(Booking booking);

    @Named("toStatus")
    default BookingDto.StatusEnum toStatus(BookingStatus status) {
        return status == null ? null : BookingDto.StatusEnum.fromValue(status.name());
    }
}
