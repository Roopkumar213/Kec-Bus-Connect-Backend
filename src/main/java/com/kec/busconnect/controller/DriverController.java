package com.kec.busconnect.controller;

import com.kec.busconnect.dto.BusLocationResponse;
import com.kec.busconnect.dto.LocationRequest;
import com.kec.busconnect.dto.PassengerSummaryResponse;
import com.kec.busconnect.model.BusLocation;
import com.kec.busconnect.model.Trip;
import com.kec.busconnect.security.UserPrincipal;
import com.kec.busconnect.service.BusService;
import com.kec.busconnect.service.LocationService;
import com.kec.busconnect.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/driver")
@PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
public class DriverController {

    private final TripService tripService;
    private final LocationService locationService;
    private final BusService busService;

    public DriverController(TripService tripService, LocationService locationService, BusService busService) {
        this.tripService = tripService;
        this.locationService = locationService;
        this.busService = busService;
    }

    @PostMapping("/trips/start")
    public ResponseEntity<Trip> startTrip(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String busId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String targetBusId = busId;
        if (targetBusId == null && body != null) {
            targetBusId = body.get("busId");
            if (targetBusId == null) {
                targetBusId = body.get("busNumber");
            }
        }
        if (targetBusId == null) {
            targetBusId = "KEC-07"; // default if not specified
        }

        Trip trip = tripService.startTrip(targetBusId, principal.getUser());
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/trips/{tripId}/stop")
    public ResponseEntity<Trip> stopTrip(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Trip trip = tripService.stopTrip(tripId, principal.getUser());
        return ResponseEntity.ok(trip);
    }

    @PostMapping("/buses/{busId}/location")
    public ResponseEntity<BusLocationResponse> updateLocation(
            @PathVariable String busId,
            @Valid @RequestBody LocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BusLocation location = locationService.updateBusLocation(busId, request, principal.getUser());

        BusLocationResponse.LatLng latLng = new BusLocationResponse.LatLng(
                location.getLocation().getCoordinates().get(1),
                location.getLocation().getCoordinates().get(0)
        );

        BusLocationResponse response = new BusLocationResponse(
                busId,
                "RUNNING",
                latLng,
                location.getAccuracy(),
                location.getSpeed(),
                location.getHeading(),
                location.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/trips/{tripId}/passenger-request")
    public ResponseEntity<Trip> requestPassengerConfirmation(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Trip trip = tripService.requestPassengerConfirmation(tripId, principal.getUser());
        return ResponseEntity.ok(trip);
    }

    @GetMapping("/trips/{tripId}/passengers")
    public ResponseEntity<PassengerSummaryResponse> getPassengers(
            @PathVariable String tripId
    ) {
        PassengerSummaryResponse response = tripService.getPassengerSummary(tripId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trips/active/{busId}")
    public ResponseEntity<Trip> getActiveTrip(
            @PathVariable String busId
    ) {
        return tripService.getActiveTripForBus(busId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
