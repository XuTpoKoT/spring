package com.example.booking.client;

import org.springframework.context.annotation.Bean;

import com.example.booking.config.HotelsClientProperties;

import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;

public class HotelsFeignConfiguration {

    @Bean
    public Request.Options hotelsRequestOptions(HotelsClientProperties properties) {
        return new Request.Options(properties.connectTimeout(), properties.readTimeout(), true);
    }

    @Bean
    public Retryer hotelsRetryer(HotelsClientProperties properties) {
        long initialBackoff = Math.max(1, properties.initialBackoff().toMillis());
        int maxAttempts = Math.max(1, properties.maxAttempts());
        return new ExponentialBackoffRetryer(initialBackoff, properties.backoffMultiplier(), maxAttempts);
    }

    @Bean
    public ErrorDecoder hotelsErrorDecoder() {
        return new HotelsFeignErrorDecoder();
    }
}
