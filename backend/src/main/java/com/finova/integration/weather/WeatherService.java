package com.finova.integration.weather;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Branch-weather helper used by the UI for localised greetings / ATM advisories.
 * Sandbox returns a fixed Mumbai snapshot; live mode would call an external weather API.
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final boolean sandbox;

    public WeatherService(@Value("${finova.integrations.mode:sandbox}") String mode) {
        this.sandbox = !"live".equalsIgnoreCase(mode);
    }

    @CircuitBreaker(name = "weatherApi", fallbackMethod = "fallback")
    public WeatherSnapshot current(String city) {
        String place = (city == null || city.isBlank()) ? "Mumbai" : city.trim();
        if (!sandbox) {
            throw new IllegalStateException("Live weather API is not configured");
        }
        log.debug("Returning sandbox weather for {}", place);
        return new WeatherSnapshot(place, 31.5, "Partly cloudy", "sandbox");
    }

    @SuppressWarnings("unused")
    private WeatherSnapshot fallback(String city, Throwable ex) {
        return new WeatherSnapshot(city == null ? "Unknown" : city, 0.0, "Unavailable", "fallback");
    }

    public record WeatherSnapshot(String city, double temperatureC, String condition, String source) {
        public Map<String, Object> asMap() {
            return Map.of(
                    "city", city,
                    "temperatureC", temperatureC,
                    "condition", condition,
                    "source", source
            );
        }
    }
}
