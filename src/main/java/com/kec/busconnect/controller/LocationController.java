package com.kec.busconnect.controller;

import com.kec.busconnect.dto.BusLocationResponse;
import com.kec.busconnect.dto.LocationRequest;
import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.BusLocation;
import com.kec.busconnect.security.UserPrincipal;
import com.kec.busconnect.service.BusService;
import com.kec.busconnect.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buses/{busId}")
public class LocationController {

    private final LocationService locationService;
    private final BusService busService;

    public LocationController(LocationService locationService, BusService busService) {
        this.locationService = locationService;
        this.busService = busService;
    }

    @GetMapping("/location")
    public ResponseEntity<BusLocationResponse> getBusLocation(@PathVariable String busId) {
        Bus bus = busService.getBusById(busId);
        BusLocation location = locationService.getBusLocation(busId);

        BusLocationResponse.LatLng latLng = new BusLocationResponse.LatLng(
                location.getLocation().getCoordinates().get(1),
                location.getLocation().getCoordinates().get(0)
        );

        BusLocationResponse response = new BusLocationResponse(
                bus.getBusNumber(),
                bus.getStatus().name(),
                latLng,
                location.getAccuracy(),
                location.getSpeed(),
                location.getHeading(),
                location.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/location")
    public ResponseEntity<BusLocationResponse> updateBusLocation(
            @PathVariable String busId,
            @Valid @RequestBody LocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BusLocation location = locationService.updateBusLocation(busId, request, principal.getUser());
        Bus bus = busService.getBusById(busId);

        BusLocationResponse.LatLng latLng = new BusLocationResponse.LatLng(
                location.getLocation().getCoordinates().get(1),
                location.getLocation().getCoordinates().get(0)
        );

        BusLocationResponse response = new BusLocationResponse(
                bus.getBusNumber(),
                bus.getStatus().name(),
                latLng,
                location.getAccuracy(),
                location.getSpeed(),
                location.getHeading(),
                location.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/start")
    public ResponseEntity<Bus> startTrip(@PathVariable String busId, @AuthenticationPrincipal UserPrincipal principal) {
        Bus bus = locationService.startTrip(busId, principal.getUser());
        return ResponseEntity.ok(bus);
    }

    @PostMapping("/stop")
    public ResponseEntity<Bus> stopTrip(@PathVariable String busId, @AuthenticationPrincipal UserPrincipal principal) {
        Bus bus = locationService.stopTrip(busId, principal.getUser());
        return ResponseEntity.ok(bus);
    }
}
