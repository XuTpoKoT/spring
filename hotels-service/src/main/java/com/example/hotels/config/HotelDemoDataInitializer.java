package com.example.hotels.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed-enabled", havingValue = "true", matchIfMissing = true)
public class HotelDemoDataInitializer implements ApplicationRunner {

    private final HotelDemoDataSeeder demoDataSeeder;

    @Override
    public void run(ApplicationArguments args) {
        demoDataSeeder.seed();
    }
}
