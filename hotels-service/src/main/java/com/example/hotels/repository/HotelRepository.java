package com.example.hotels.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hotels.entity.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    boolean existsByName(String name);

    List<Hotel> findAllByOrderByIdAsc();
}
