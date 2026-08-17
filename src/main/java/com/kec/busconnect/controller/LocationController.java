package com.kec.busconnect.controller;

import com.kec.busconnect.dto.BusLocationResponse;
import com.kec.busconnect.dto.LocationRequest;
import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.BusLocation;
import com.kec.busconnect.model.Trip;
import com.kec.busconnect.security.UserPrincipal;
import com.kec.busconnect.service.BusService;
import com.kec.busconnect.service.LocationService;
import com.kec.busconnect.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buses/{busId}")
public class LocationController {

    private final LocationService locationService;
    private final BusService busService;
    private final TripService tripService;

    public LocationController(LocationService locationService, BusService busService, TripService tripService) {
        this.locationService = locationService;
        this.busService = busService;
        this.tripService = tripService;
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
    public ResponseEntity<Trip> startTrip(@PathVariable String busId, @AuthenticationPrincipal UserPrincipal principal) {
        Trip trip = tripService.startTrip(busId, principal.getUser());
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/stop")
    public ResponseEntity<Trip> stopTrip(@PathVariable String busId, @AuthenticationPrincipal UserPrincipal principal) {
        // If busId is passed, find active trip or stop by id
        Trip trip = tripService.getActiveTripForBus(busId)
                .map(t -> tripService.stopTrip(t.getId(), principal.getUser()))
                .orElseGet(() -> tripService.stopTrip(busId, principal.getUser()));
        return ResponseEntity.ok(trip);
    }
}
