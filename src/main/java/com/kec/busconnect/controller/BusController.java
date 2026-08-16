package com.kec.busconnect.controller;

import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.Route;
import com.kec.busconnect.service.BusService;
import com.kec.busconnect.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/buses")
public class BusController {

    private final BusService busService;
    private final RouteService routeService;

    public BusController(BusService busService, RouteService routeService) {
        this.busService = busService;
        this.routeService = routeService;
    }

    @GetMapping
    public ResponseEntity<List<Bus>> getActiveBuses() {
        List<Bus> activeBuses = busService.getActiveBuses();
        return ResponseEntity.ok(activeBuses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBusDetails(@PathVariable String id) {
        Bus bus = busService.getBusById(id);
        Route route = null;
        if (bus.getRouteId() != null) {
            try {
                route = routeService.getRouteById(bus.getRouteId());
            } catch (Exception e) {
                // Ignore missing route
            }
        }

        Map<String, Object> details = new HashMap<>();
        details.put("id", bus.getId());
        details.put("busNumber", bus.getBusNumber());
        details.put("registrationNumber", bus.getRegistrationNumber());
        details.put("trackerId", bus.getTrackerId());
        details.put("status", bus.getStatus());
        details.put("isActive", bus.isActive());
        details.put("route", route);

        return ResponseEntity.ok(details);
    }
}
