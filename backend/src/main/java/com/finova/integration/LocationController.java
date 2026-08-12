package com.finova.integration;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finova.integration.maps.MapsService;
import com.finova.integration.weather.WeatherService;

@RestController
@RequestMapping("/api/v1")
public class LocationController {

    private final WeatherService weatherService;
    private final MapsService mapsService;

    public LocationController(WeatherService weatherService, MapsService mapsService) {
        this.weatherService = weatherService;
        this.mapsService = mapsService;
    }

    @GetMapping("/weather")
    public ResponseEntity<Map<String, Object>> weather(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(weatherService.current(city).asMap());
    }

    @GetMapping("/branches/nearby")
    public ResponseEntity<List<MapsService.BranchLocation>> branches(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(mapsService.nearbyBranches(city));
    }
}
