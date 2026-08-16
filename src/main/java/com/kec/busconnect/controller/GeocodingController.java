package com.kec.busconnect.controller;

import com.kec.busconnect.dto.GeocodeResponse;
import com.kec.busconnect.service.GeocodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping("/reverse")
    public ResponseEntity<GeocodeResponse> reverseGeocode(
            @RequestParam("lat") Double latitude,
            @RequestParam("lng") Double longitude
    ) {
        GeocodeResponse response = geocodingService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(response);
    }
}
