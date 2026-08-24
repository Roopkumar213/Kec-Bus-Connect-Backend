package com.kec.busconnect.controller;

import com.kec.busconnect.dto.LiveBusStatusResponse;
import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.Route;
import com.kec.busconnect.service.BusService;
import com.kec.busconnect.service.LocationService;
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
    private final LocationService locationService;

    public BusController(BusService busService, RouteService routeService, LocationService locationService) {
        this.busService = busService;
        this.routeService = routeService;
        this.locationService = locationService;
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
        details.put("routeId", bus.getRouteId());
        details.put("route", route);

        return ResponseEntity.ok(details);
    }

    @GetMapping("/{id}/live")
    public ResponseEntity<LiveBusStatusResponse> getLiveBusStatus(@PathVariable String id) {
        LiveBusStatusResponse liveStatus = locationService.getLiveBusStatus(id);
        return ResponseEntity.ok(liveStatus);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getBusStatus(@PathVariable String id) {
        Bus bus = busService.getBusById(id);
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("busId", bus.getId());
        statusMap.put("busNumber", bus.getBusNumber());
        statusMap.put("status", bus.getStatus());
        statusMap.put("activeTrip", locationService.getActiveTripForBus(bus.getId()).isPresent());
        return ResponseEntity.ok(statusMap);
    }
}
