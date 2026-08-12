package com.finova.common.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight liveness endpoint used for smoke tests and container health checks.
 *
 * <p>Kept separate from Spring Actuator's {@code /actuator/health} so it can remain
 * publicly reachable (no auth) without loosening actuator exposure rules.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "service", "finova-banking",
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}
