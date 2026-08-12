package com.finova.integration.maps;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/** Nearby-branch / ATM lookup. Sandbox returns a fixed Mumbai branch set. */
@Service
public class MapsService {

    private static final Logger log = LoggerFactory.getLogger(MapsService.class);

    private final boolean sandbox;

    public MapsService(@Value("${finova.integrations.mode:sandbox}") String mode) {
        this.sandbox = !"live".equalsIgnoreCase(mode);
    }

    @CircuitBreaker(name = "mapsApi", fallbackMethod = "fallback")
    public List<BranchLocation> nearbyBranches(String city) {
        String place = (city == null || city.isBlank()) ? "Mumbai" : city.trim();
        if (!sandbox) {
            throw new IllegalStateException("Live maps API is not configured");
        }
        log.debug("Returning sandbox branches for {}", place);
        return List.of(
                new BranchLocation("Finova Bandra West", place, 19.0596, 72.8295, "BRANCH"),
                new BranchLocation("Finova ATM Andheri", place, 19.1197, 72.8468, "ATM")
        );
    }

    @SuppressWarnings("unused")
    private List<BranchLocation> fallback(String city, Throwable ex) {
        return List.of();
    }

    public record BranchLocation(String name, String city, double lat, double lng, String type) {
    }
}
