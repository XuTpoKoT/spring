package com.example.hotels.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rooms")
public record RoomProperties(Duration lockTtl) {
}
