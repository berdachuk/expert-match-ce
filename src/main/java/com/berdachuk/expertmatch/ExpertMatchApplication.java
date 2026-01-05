package com.berdachuk.expertmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * ExpertMatch Application
 * <p>
 * Enterprise-grade expert discovery and team formation system
 * that matches project requirements with qualified experts.
 */
@SpringBootApplication
public class ExpertMatchApplication {

    public static void main(String[] args) {
        // Set default timezone to Minsk (UTC+3)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Minsk"));

        SpringApplication.run(ExpertMatchApplication.class, args);
    }
}

