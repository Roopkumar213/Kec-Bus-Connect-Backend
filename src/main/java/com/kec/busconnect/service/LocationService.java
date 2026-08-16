package com.kec.busconnect.service;

import com.kec.busconnect.dto.BusLocationResponse;
import com.kec.busconnect.dto.LocationRequest;
import com.kec.busconnect.enums.BusStatus;
import com.kec.busconnect.enums.Role;
import com.kec.busconnect.enums.TripStatus;
import com.kec.busconnect.exception.BadRequestException;
import com.kec.busconnect.exception.ResourceNotFoundException;
import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.BusLocation;
import com.kec.busconnect.model.GeoPoint;
import com.kec.busconnect.model.Trip;
import com.kec.busconnect.model.User;
import com.kec.busconnect.repository.BusLocationRepository;
import com.kec.busconnect.repository.BusRepository;
import com.kec.busconnect.repository.TripRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Service
public class LocationService {

    private final BusRepository busRepository;
    private final BusLocationRepository busLocationRepository;
    private final TripRepository tripRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LocationService(BusRepository busRepository,
                           BusLocationRepository busLocationRepository,
                           TripRepository tripRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.busRepository = busRepository;
        this.busLocationRepository = busLocationRepository;
        this.tripRepository = tripRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public BusLocation getBusLocation(String busId) {
        return busLocationRepository.findByBusId(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Location details not found for bus ID: " + busId));
    }

    public Optional<Trip> getActiveTripForBus(String busId) {
        return tripRepository.findFirstByBusIdAndStatus(busId, TripStatus.ACTIVE);
    }

    @Transactional
    public BusLocation updateBusLocation(String busId, LocationRequest request, User currentUser) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID: " + busId));

        if (currentUser.getRole() != Role.ADMIN) {
            if (bus.getTrackerId() == null || !bus.getTrackerId().equals(currentUser.getId())) {
                throw new BadRequestException("You are not authorized to update coordinates for this bus");
            }
        }

        BusLocation busLocation = busLocationRepository.findByBusId(busId)
                .orElseGet(() -> {
                    BusLocation loc = new BusLocation();
                    loc.setBusId(busId);
                    return loc;
                });

        GeoPoint point = new GeoPoint(
                "Point",
                Arrays.asList(request.getLongitude(), request.getLatitude())
        );
        busLocation.setLocation(point);
        busLocation.setAccuracy(request.getAccuracy());
        busLocation.setSpeed(request.getSpeed());
        busLocation.setHeading(request.getHeading());
        busLocation.setUpdatedAt(Instant.now());

        if (bus.getStatus() == BusStatus.NOT_STARTED || bus.getStatus() == BusStatus.OFFLINE) {
            bus.setStatus(BusStatus.RUNNING);
            busRepository.save(bus);
        }

        BusLocation savedLocation = busLocationRepository.save(busLocation);

        // Update or create active Trip record
        Trip activeTrip = tripRepository.findFirstByBusIdAndStatus(busId, TripStatus.ACTIVE)
                .orElseGet(() -> {
                    Trip trip = new Trip();
                    trip.setBusId(busId);
                    trip.setBusNumber(bus.getBusNumber());
                    trip.setDriverId(currentUser.getId());
                    trip.setRouteId(bus.getRouteId());
                    trip.setStatus(TripStatus.ACTIVE);
                    trip.setStartTime(Instant.now());
                    return trip;
                });

        activeTrip.setLastLocation(point);
        activeTrip.setLastSpeed(request.getSpeed());
        activeTrip.setLastAccuracy(request.getAccuracy());
        activeTrip.setLastHeading(request.getHeading());
        activeTrip.setLastUpdated(Instant.now());
        tripRepository.save(activeTrip);

        // Broadcast to WebSocket subscribers
        BusLocationResponse.LatLng latLng = new BusLocationResponse.LatLng(
                request.getLatitude(),
                request.getLongitude()
        );
        BusLocationResponse broadcastPayload = new BusLocationResponse(
                bus.getBusNumber(),
                bus.getStatus().name(),
                latLng,
                request.getAccuracy(),
                request.getSpeed(),
                request.getHeading(),
                savedLocation.getUpdatedAt()
        );

        try {
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getBusNumber(), broadcastPayload);
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getId(), broadcastPayload);
            messagingTemplate.convertAndSend("/topic/buses", broadcastPayload);
        } catch (Exception e) {
            System.err.println("WebSocket broadcast warning: " + e.getMessage());
        }

        return savedLocation;
    }

    @Transactional
    public Bus startTrip(String busId, User currentUser) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID: " + busId));

        if (currentUser.getRole() != Role.ADMIN) {
            if (bus.getTrackerId() == null || !bus.getTrackerId().equals(currentUser.getId())) {
                throw new BadRequestException("You are not authorized to start trip for this bus");
            }
        }

        bus.setStatus(BusStatus.RUNNING);
        Bus savedBus = busRepository.save(bus);

        // Create new active trip if one doesn't exist
        Trip activeTrip = tripRepository.findFirstByBusIdAndStatus(busId, TripStatus.ACTIVE)
                .orElseGet(() -> {
                    Trip trip = new Trip();
                    trip.setBusId(busId);
                    trip.setBusNumber(bus.getBusNumber());
                    trip.setDriverId(currentUser.getId());
                    trip.setRouteId(bus.getRouteId());
                    trip.setStatus(TripStatus.ACTIVE);
                    trip.setStartTime(Instant.now());
                    trip.setLastUpdated(Instant.now());
                    return tripRepository.save(trip);
                });

        try {
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getBusNumber() + "/status", "RUNNING");
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getId() + "/status", "RUNNING");
        } catch (Exception e) {
            // Ignore messaging errors
        }

        return savedBus;
    }

    @Transactional
    public Bus stopTrip(String busId, User currentUser) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with ID: " + busId));

        if (currentUser.getRole() != Role.ADMIN) {
            if (bus.getTrackerId() == null || !bus.getTrackerId().equals(currentUser.getId())) {
                throw new BadRequestException("You are not authorized to stop trip for this bus");
            }
        }

        bus.setStatus(BusStatus.COMPLETED);
        Bus savedBus = busRepository.save(bus);

        // End any active trip
        tripRepository.findFirstByBusIdAndStatus(busId, TripStatus.ACTIVE).ifPresent(trip -> {
            trip.setStatus(TripStatus.COMPLETED);
            trip.setEndTime(Instant.now());
            trip.setLastUpdated(Instant.now());
            tripRepository.save(trip);
        });

        try {
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getBusNumber() + "/status", "COMPLETED");
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getId() + "/status", "COMPLETED");
        } catch (Exception e) {
            // Ignore messaging errors
        }

        return savedBus;
    }

    @Transactional
    public boolean recordStudentBoarding(String busId, String studentId) {
        Bus bus = busRepository.findById(busId)
                .orElseGet(() -> busRepository.findByBusNumber(busId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bus not found: " + busId)));

        Trip activeTrip = tripRepository.findFirstByBusIdAndStatus(bus.getId(), TripStatus.ACTIVE)
                .orElseGet(() -> {
                    Trip trip = new Trip();
                    trip.setBusId(bus.getId());
                    trip.setBusNumber(bus.getBusNumber());
                    trip.setStatus(TripStatus.ACTIVE);
                    trip.setStartTime(Instant.now());
                    return tripRepository.save(trip);
                });

        boolean added = activeTrip.getBoardedStudentIds().add(studentId);
        tripRepository.save(activeTrip);

        try {
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getBusNumber() + "/boarding", activeTrip.getBoardedStudentIds().size());
        } catch (Exception e) {
            // Ignore
        }

        return added;
    }
}
