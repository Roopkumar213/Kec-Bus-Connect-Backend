package com.kec.busconnect.service;

import com.kec.busconnect.dto.PassengerSummaryResponse;
import com.kec.busconnect.enums.BusStatus;
import com.kec.busconnect.enums.PassengerStatus;
import com.kec.busconnect.enums.Role;
import com.kec.busconnect.enums.TripDirection;
import com.kec.busconnect.enums.TripStatus;
import com.kec.busconnect.exception.BadRequestException;
import com.kec.busconnect.exception.ResourceNotFoundException;
import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.BusLocation;
import com.kec.busconnect.model.GeoPoint;
import com.kec.busconnect.model.PassengerConfirmation;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.model.Trip;
import com.kec.busconnect.model.User;
import com.kec.busconnect.repository.BusLocationRepository;
import com.kec.busconnect.repository.BusRepository;
import com.kec.busconnect.repository.StudentRepository;
import com.kec.busconnect.repository.TripRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final BusRepository busRepository;
    private final BusLocationRepository busLocationRepository;
    private final StudentRepository studentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TripService(TripRepository tripRepository,
                       BusRepository busRepository,
                       BusLocationRepository busLocationRepository,
                       StudentRepository studentRepository,
                       SimpMessagingTemplate messagingTemplate) {
        this.tripRepository = tripRepository;
        this.busRepository = busRepository;
        this.busLocationRepository = busLocationRepository;
        this.studentRepository = studentRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Optional<Trip> getActiveTripForBus(String busId) {
        return tripRepository.findFirstByBusIdAndStatus(busId, TripStatus.ACTIVE);
    }

    public Optional<Trip> getTripById(String tripId) {
        return tripRepository.findById(tripId);
    }

    @Transactional
    public Trip startTrip(String busId, User currentUser) {
        return startTrip(busId, currentUser, TripDirection.MORNING);
    }

    @Transactional
    public Trip startTrip(String busId, User currentUser, TripDirection direction) {
        Bus bus = busRepository.findById(busId)
                .orElseGet(() -> busRepository.findByBusNumber(busId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bus not found with identifier: " + busId)));

        if (currentUser.getRole() != Role.ADMIN) {
            if (bus.getTrackerId() == null || !bus.getTrackerId().equals(currentUser.getId())) {
                throw new BadRequestException("You are not authorized to start a trip for this bus");
            }
        }

        TripDirection effectiveDirection = direction != null ? direction : TripDirection.MORNING;

        // Check if active trip already exists
        Optional<Trip> existing = tripRepository.findFirstByBusIdAndStatus(bus.getId(), TripStatus.ACTIVE);
        if (existing.isPresent()) {
            Trip existingTrip = existing.get();
            // If direction is explicitly provided and different, update it
            if (direction != null && existingTrip.getDirection() != direction) {
                existingTrip.setDirection(direction);
                existingTrip.setLastUpdated(Instant.now());
                existingTrip = tripRepository.save(existingTrip);
            }
            bus.setStatus(BusStatus.RUNNING);
            busRepository.save(bus);
            return existingTrip;
        }

        bus.setStatus(BusStatus.RUNNING);
        busRepository.save(bus);

        Trip trip = new Trip();
        trip.setBusId(bus.getId());
        trip.setBusNumber(bus.getBusNumber());
        trip.setDriverId(currentUser.getId());
        trip.setRouteId(bus.getRouteId());
        trip.setStatus(TripStatus.ACTIVE);
        trip.setDirection(effectiveDirection);
        trip.setRemindedStudentIds(new HashSet<>());
        trip.setStartTime(Instant.now());
        trip.setLastUpdated(Instant.now());
        trip.setPassengerRequestActive(false);

        // Set initial starting coordinates based on Trip Direction
        // For EVENING: starts from College (KEC: [78.360311, 12.721662])
        // For MORNING: starts from Attikuppam Origin ([78.479812, 12.884713])
        GeoPoint startPoint = (effectiveDirection == TripDirection.EVENING)
                ? new GeoPoint("Point", Arrays.asList(78.360311, 12.721662))
                : new GeoPoint("Point", Arrays.asList(78.479812, 12.884713));

        trip.setLastLocation(startPoint);
        trip.setLastSpeed(0.0);
        trip.setLastAccuracy(8.0);
        trip.setLastHeading(0.0);

        // Update busLocation record as well
        BusLocation busLocation = busLocationRepository.findByBusId(bus.getId()).orElse(new BusLocation());
        busLocation.setBusId(bus.getId());
        busLocation.setLocation(startPoint);
        busLocation.setAccuracy(8.0);
        busLocation.setSpeed(0.0);
        busLocation.setHeading(0.0);
        busLocation.setUpdatedAt(Instant.now());
        busLocationRepository.save(busLocation);

        // Pre-populate passenger list with assigned students
        List<Student> assignedStudents = studentRepository.findByAssignedBus(bus.getId());
        if (assignedStudents.isEmpty() && bus.getRouteId() != null) {
            assignedStudents = studentRepository.findByAssignedRoute(bus.getRouteId());
        }

        List<PassengerConfirmation> confirmations = new ArrayList<>();
        for (Student s : assignedStudents) {
            PassengerConfirmation pc = new PassengerConfirmation();
            pc.setStudentId(s.getId());
            pc.setStudentName(s.getFullName());
            pc.setStudentRollNumber(s.getStudentId());
            pc.setStatus(PassengerStatus.PENDING);
            confirmations.add(pc);
        }
        trip.setPassengerConfirmations(confirmations);

        Trip savedTrip = tripRepository.save(trip);

        try {
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getBusNumber() + "/status", "RUNNING");
            messagingTemplate.convertAndSend("/topic/bus/" + bus.getId() + "/status", "RUNNING");
            messagingTemplate.convertAndSend("/topic/buses", "RUNNING");
        } catch (Exception e) {
            // Ignore messaging errors
        }

        return savedTrip;
    }

    @Transactional
    public Trip stopTrip(String tripId, User currentUser) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        if (currentUser.getRole() != Role.ADMIN) {
            boolean isAssignedDriver = false;
            Optional<Bus> busOpt = busRepository.findById(trip.getBusId());
            if (busOpt.isPresent()) {
                Bus bus = busOpt.get();
                if (currentUser.getId().equals(bus.getTrackerId())) {
                    isAssignedDriver = true;
                }
            }

            if (!isAssignedDriver && (trip.getDriverId() == null || !trip.getDriverId().equals(currentUser.getId()))) {
                throw new BadRequestException("You are not authorized to stop this trip");
            }
        }

        trip.setStatus(TripStatus.COMPLETED);
        trip.setEndTime(Instant.now());
        trip.setLastUpdated(Instant.now());
        trip.setPassengerRequestActive(false);

        // Expire any pending passenger requests
        if (trip.getPassengerConfirmations() != null) {
            for (PassengerConfirmation pc : trip.getPassengerConfirmations()) {
                if (pc.getStatus() == PassengerStatus.PENDING) {
                    pc.setStatus(PassengerStatus.EXPIRED);
                }
            }
        }

        Trip savedTrip = tripRepository.save(trip);

        // Update bus status to COMPLETED
        busRepository.findById(trip.getBusId()).ifPresent(bus -> {
            bus.setStatus(BusStatus.COMPLETED);
            busRepository.save(bus);
            try {
                messagingTemplate.convertAndSend("/topic/bus/" + bus.getBusNumber() + "/status", "COMPLETED");
                messagingTemplate.convertAndSend("/topic/bus/" + bus.getId() + "/status", "COMPLETED");
            } catch (Exception e) {
                // Ignore
            }
        });

        return savedTrip;
    }

    @Transactional
    public Trip requestPassengerConfirmation(String tripId, User currentUser) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        if (currentUser.getRole() != Role.ADMIN) {
            if (trip.getDriverId() == null || !trip.getDriverId().equals(currentUser.getId())) {
                throw new BadRequestException("You are not authorized to request confirmations for this trip");
            }
        }

        trip.setPassengerRequestActive(true);
        trip.setPassengerRequestTimestamp(Instant.now());
        trip.setLastUpdated(Instant.now());

        Trip savedTrip = tripRepository.save(trip);

        // Broadcast to WebSocket topics
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PASSENGER_CONFIRMATION_REQUEST");
        payload.put("tripId", trip.getId());
        payload.put("busNumber", trip.getBusNumber());
        payload.put("message", "Driver has requested passenger confirmation.");
        payload.put("timestamp", Instant.now().toString());

        try {
            messagingTemplate.convertAndSend("/topic/trip/" + trip.getId() + "/passenger-request", payload);
            messagingTemplate.convertAndSend("/topic/bus/" + trip.getBusNumber() + "/passenger-request", payload);
        } catch (Exception e) {
            System.err.println("Failed to broadcast passenger request: " + e.getMessage());
        }

        return savedTrip;
    }

    @Transactional
    public PassengerConfirmation updatePassengerStatus(String tripId, String studentUserId, PassengerStatus status) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + studentUserId));

        List<PassengerConfirmation> list = trip.getPassengerConfirmations();
        if (list == null) {
            list = new ArrayList<>();
            trip.setPassengerConfirmations(list);
        }

        Optional<PassengerConfirmation> existing = list.stream()
                .filter(p -> p.getStudentId() != null && p.getStudentId().equals(student.getId()))
                .findFirst();

        PassengerConfirmation confirmation;
        if (existing.isPresent()) {
            confirmation = existing.get();
            confirmation.setStatus(status);
            confirmation.setConfirmedAt(Instant.now());
        } else {
            confirmation = new PassengerConfirmation(
                    student.getId(),
                    student.getFullName(),
                    student.getStudentId(),
                    null,
                    status,
                    Instant.now()
            );
            list.add(confirmation);
        }

        if (status == PassengerStatus.CONFIRMED_ON_BUS) {
            trip.getBoardedStudentIds().add(student.getId());
        } else if (status == PassengerStatus.NOT_ON_BUS) {
            trip.getBoardedStudentIds().remove(student.getId());
        }

        trip.setLastUpdated(Instant.now());
        tripRepository.save(trip);

        // Broadcast updated passenger summary to driver and bus subscribers
        PassengerSummaryResponse summary = getPassengerSummary(tripId);
        try {
            messagingTemplate.convertAndSend("/topic/trip/" + tripId + "/passengers", summary);
            messagingTemplate.convertAndSend("/topic/bus/" + trip.getBusNumber() + "/passengers", summary);
        } catch (Exception e) {
            // Ignore messaging errors
        }

        return confirmation;
    }

    public PassengerSummaryResponse getPassengerSummary(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        List<PassengerConfirmation> list = trip.getPassengerConfirmations() != null ? trip.getPassengerConfirmations() : Collections.emptyList();

        int confirmed = 0;
        int notResponded = 0;
        int notOnBus = 0;

        for (PassengerConfirmation p : list) {
            if (p.getStatus() == PassengerStatus.CONFIRMED_ON_BUS) {
                confirmed++;
            } else if (p.getStatus() == PassengerStatus.NOT_ON_BUS) {
                notOnBus++;
            } else {
                notResponded++;
            }
        }

        return new PassengerSummaryResponse(
                trip.getId(),
                trip.getBusNumber(),
                confirmed,
                notResponded,
                notOnBus,
                list.size(),
                trip.isPassengerRequestActive(),
                list
        );
    }
}
