package com.kec.busconnect.service;

import com.kec.busconnect.dto.BusLocationResponse;
import com.kec.busconnect.dto.LiveBusStatusResponse;
import com.kec.busconnect.dto.LocationRequest;
import com.kec.busconnect.enums.BusStatus;
import com.kec.busconnect.enums.Role;
import com.kec.busconnect.enums.TripStatus;
import com.kec.busconnect.exception.BadRequestException;
import com.kec.busconnect.exception.ResourceNotFoundException;
import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.BusLocation;
import com.kec.busconnect.model.GeoPoint;
import com.kec.busconnect.model.Route;
import com.kec.busconnect.model.Trip;
import com.kec.busconnect.model.User;
import com.kec.busconnect.repository.BusLocationRepository;
import com.kec.busconnect.repository.BusRepository;
import com.kec.busconnect.repository.RouteRepository;
import com.kec.busconnect.repository.TripRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

    public static final double CURRENT_STOP_RADIUS_METERS = 250.0;
    public static final long FRESHNESS_LIVE_SECONDS = 30;
    public static final long FRESHNESS_STALE_SECONDS = 180;

    private final BusRepository busRepository;
    private final BusLocationRepository busLocationRepository;
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LocationService(BusRepository busRepository,
                           BusLocationRepository busLocationRepository,
                           TripRepository tripRepository,
                           RouteRepository routeRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.busRepository = busRepository;
        this.busLocationRepository = busLocationRepository;
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public BusLocation getBusLocation(String busId) {
        return busLocationRepository.findByBusId(busId)
                .orElseGet(() -> {
                    Bus bus = busRepository.findByBusNumber(busId).orElse(null);
                    if (bus != null) {
                        return busLocationRepository.findByBusId(bus.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Location details not found for bus: " + busId));
                    }
                    throw new ResourceNotFoundException("Location details not found for bus ID: " + busId);
                });
    }

    public Optional<Trip> getActiveTripForBus(String busId) {
        return tripRepository.findFirstByBusIdAndStatus(busId, TripStatus.ACTIVE);
    }

    public LiveBusStatusResponse getLiveBusStatus(String busIdentifier) {
        Bus bus = busRepository.findById(busIdentifier)
                .orElseGet(() -> busRepository.findByBusNumber(busIdentifier)
                        .orElseThrow(() -> new ResourceNotFoundException("Bus not found: " + busIdentifier)));

        BusLocation loc = busLocationRepository.findByBusId(bus.getId()).orElse(null);
        Optional<Trip> activeTrip = tripRepository.findFirstByBusIdAndStatus(bus.getId(), TripStatus.ACTIVE);

        Double lat = null;
        Double lng = null;
        Double accuracy = null;
        Double speed = 0.0;
        Double heading = 0.0;
        Instant updatedAt = null;
        Long secondsSinceUpdate = null;
        String freshness = "LOCATION_DELAYED";

        if (loc != null && loc.getLocation() != null && loc.getLocation().getCoordinates() != null && loc.getLocation().getCoordinates().size() >= 2) {
            lng = loc.getLocation().getCoordinates().get(0);
            lat = loc.getLocation().getCoordinates().get(1);
            accuracy = loc.getAccuracy();
            speed = loc.getSpeed() != null ? loc.getSpeed() : 0.0;
            heading = loc.getHeading() != null ? loc.getHeading() : 0.0;
            updatedAt = loc.getUpdatedAt();

            if (updatedAt != null) {
                secondsSinceUpdate = Duration.between(updatedAt, Instant.now()).getSeconds();
                if (secondsSinceUpdate <= FRESHNESS_LIVE_SECONDS) {
                    freshness = "LIVE";
                } else if (secondsSinceUpdate <= FRESHNESS_STALE_SECONDS) {
                    freshness = "STALE";
                } else {
                    freshness = "LOCATION_DELAYED";
                }
            }
        }

        // Calculate stop proximity from route
        String currentlyAtStop = null;
        String previousStop = null;
        String nextStop = null;
        Double distanceToNextStopKm = null;
        Double etaMinutes = null;

        if (lat != null && lng != null && bus.getRouteId() != null) {
            Optional<Route> routeOpt = routeRepository.findById(bus.getRouteId());
            if (routeOpt.isPresent()) {
                Route route = routeOpt.get();
                List<Route.Stop> stops = route.getStops();
                if (stops != null && !stops.isEmpty()) {
                    stops.sort(Comparator.comparing(Route.Stop::getSequence, Comparator.nullsLast(Comparator.naturalOrder())));

                    int nearestIdx = -1;
                    double minDistanceMeters = Double.MAX_VALUE;

                    for (int i = 0; i < stops.size(); i++) {
                        Route.Stop s = stops.get(i);
                        if (s.getLocation() != null && s.getLocation().getCoordinates() != null && s.getLocation().getCoordinates().size() >= 2) {
                            double sLng = s.getLocation().getCoordinates().get(0);
                            double sLat = s.getLocation().getCoordinates().get(1);
                            double dist = calculateDistanceMeters(lat, lng, sLat, sLng);
                            if (dist < minDistanceMeters) {
                                minDistanceMeters = dist;
                                nearestIdx = i;
                            }
                        }
                    }

                    if (nearestIdx != -1) {
                        if (minDistanceMeters <= CURRENT_STOP_RADIUS_METERS) {
                            currentlyAtStop = stops.get(nearestIdx).getName();
                        }

                        if (nearestIdx > 0) {
                            previousStop = stops.get(nearestIdx - 1).getName();
                        }

                        int nextIdx = (minDistanceMeters <= CURRENT_STOP_RADIUS_METERS) ? nearestIdx + 1 : nearestIdx;
                        if (nextIdx < stops.size()) {
                            Route.Stop nStop = stops.get(nextIdx);
                            nextStop = nStop.getName();
                            double nLng = nStop.getLocation().getCoordinates().get(0);
                            double nLat = nStop.getLocation().getCoordinates().get(1);
                            double dMeters = calculateDistanceMeters(lat, lng, nLat, nLng);
                            distanceToNextStopKm = Math.round((dMeters / 1000.0) * 10.0) / 10.0;

                            if (speed > 0) {
                                etaMinutes = Math.round((distanceToNextStopKm / speed) * 60.0 * 10.0) / 10.0;
                            }
                        }
                    }
                }
            }
        }

        return new LiveBusStatusResponse(
                bus.getId(),
                bus.getBusNumber(),
                bus.getRegistrationNumber(),
                bus.getStatus().name(),
                freshness,
                lat,
                lng,
                accuracy,
                speed,
                heading,
                updatedAt,
                secondsSinceUpdate,
                currentlyAtStop,
                previousStop,
                nextStop,
                distanceToNextStopKm,
                etaMinutes,
                activeTrip.map(Trip::getId).orElse(null),
                activeTrip.map(Trip::isPassengerRequestActive).orElse(false)
        );
    }

    @Transactional
    public BusLocation updateBusLocation(String busId, LocationRequest request, User currentUser) {
        Bus bus = busRepository.findById(busId)
                .orElseGet(() -> busRepository.findByBusNumber(busId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bus not found: " + busId)));

        if (currentUser.getRole() != Role.ADMIN) {
            if (bus.getTrackerId() == null || !bus.getTrackerId().equals(currentUser.getId())) {
                throw new BadRequestException("You are not authorized to update coordinates for this bus");
            }
        }

        BusLocation busLocation = busLocationRepository.findByBusId(bus.getId())
                .orElseGet(() -> {
                    BusLocation loc = new BusLocation();
                    loc.setBusId(bus.getId());
                    return loc;
                });

        // Compute speed fallback if GPS speed is null or 0
        Double calculatedSpeed = request.getSpeed();
        Instant now = Instant.now();

        if (calculatedSpeed == null && busLocation.getLocation() != null && busLocation.getUpdatedAt() != null) {
            double prevLng = busLocation.getLocation().getCoordinates().get(0);
            double prevLat = busLocation.getLocation().getCoordinates().get(1);
            double distMeters = calculateDistanceMeters(prevLat, prevLng, request.getLatitude(), request.getLongitude());
            long secondsElapsed = Math.max(1, Duration.between(busLocation.getUpdatedAt(), now).getSeconds());
            double speedMps = distMeters / secondsElapsed;
            calculatedSpeed = Math.round(speedMps * 3.6 * 10.0) / 10.0; // km/h
        }
        if (calculatedSpeed == null || calculatedSpeed < 0) {
            calculatedSpeed = 0.0;
        }

        // MongoDB GeoJSON Point: [longitude, latitude]
        GeoPoint point = new GeoPoint(
                "Point",
                Arrays.asList(request.getLongitude(), request.getLatitude())
        );
        busLocation.setLocation(point);
        busLocation.setAccuracy(request.getAccuracy());
        busLocation.setSpeed(calculatedSpeed);
        busLocation.setHeading(request.getHeading());
        busLocation.setUpdatedAt(now);

        if (bus.getStatus() == BusStatus.NOT_STARTED || bus.getStatus() == BusStatus.OFFLINE) {
            bus.setStatus(BusStatus.RUNNING);
            busRepository.save(bus);
        }

        BusLocation savedLocation = busLocationRepository.save(busLocation);

        // Update active trip location if present
        tripRepository.findFirstByBusIdAndStatus(bus.getId(), TripStatus.ACTIVE).ifPresent(trip -> {
            trip.setLastLocation(point);
            trip.setLastSpeed(request.getSpeed());
            trip.setLastAccuracy(request.getAccuracy());
            trip.setLastHeading(request.getHeading());
            trip.setLastUpdated(now);
            tripRepository.save(trip);
        });

        // Broadcast enriched live status to STOMP WebSocket subscribers
        LiveBusStatusResponse liveStatus = getLiveBusStatus(bus.getId());

        try {
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getBusNumber(), liveStatus);
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getId(), liveStatus);
            messagingTemplate.convertAndSend("/topic/buses", liveStatus);
        } catch (Exception e) {
            System.err.println("WebSocket broadcast warning: " + e.getMessage());
        }

        return savedLocation;
    }

    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
