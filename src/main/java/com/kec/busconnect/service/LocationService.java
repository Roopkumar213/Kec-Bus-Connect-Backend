package com.kec.busconnect.service;

import com.kec.busconnect.dto.BusLocationResponse;
import com.kec.busconnect.dto.LiveBusStatusResponse;
import com.kec.busconnect.dto.LocationRequest;
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
import com.kec.busconnect.model.Route;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.model.Trip;
import com.kec.busconnect.model.TripReminder;
import com.kec.busconnect.model.User;
import com.kec.busconnect.repository.BusLocationRepository;
import com.kec.busconnect.repository.BusRepository;
import com.kec.busconnect.repository.RouteRepository;
import com.kec.busconnect.repository.StudentRepository;
import com.kec.busconnect.repository.TripReminderRepository;
import com.kec.busconnect.repository.TripRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class LocationService {

    public static final double CURRENT_STOP_RADIUS_METERS = 250.0;
    public static final long FRESHNESS_LIVE_SECONDS = 30;
    public static final long FRESHNESS_STALE_SECONDS = 180;

    private final BusRepository busRepository;
    private final BusLocationRepository busLocationRepository;
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final StudentRepository studentRepository;
    private final TripReminderRepository tripReminderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LocationService(BusRepository busRepository,
                           BusLocationRepository busLocationRepository,
                           TripRepository tripRepository,
                           RouteRepository routeRepository,
                           StudentRepository studentRepository,
                           TripReminderRepository tripReminderRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.busRepository = busRepository;
        this.busLocationRepository = busLocationRepository;
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.studentRepository = studentRepository;
        this.tripReminderRepository = tripReminderRepository;
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

        TripDirection direction = activeTrip.map(Trip::getDirection).orElse(TripDirection.MORNING);

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

        // Calculate stop proximity from route, accounting for TripDirection
        String currentlyAtStop = null;
        String previousStop = null;
        String nextStop = null;
        Double distanceToNextStopKm = null;
        Double etaMinutes = null;
        String startingPoint = "Attikuppam (Origin)";
        String destination = "Kuppam Engineering College (KEC - Terminus)";

        if (direction == TripDirection.EVENING) {
            startingPoint = "Kuppam Engineering College (KEC - Terminus)";
            destination = "Attikuppam (Origin)";
        }

        if (lat != null && lng != null && bus.getRouteId() != null) {
            Optional<Route> routeOpt = routeRepository.findById(bus.getRouteId());
            if (routeOpt.isPresent()) {
                Route route = routeOpt.get();
                if (route.getStops() != null && !route.getStops().isEmpty()) {
                    List<Route.Stop> stops = new ArrayList<>(route.getStops());
                    stops.sort(Comparator.comparing(Route.Stop::getSequence, Comparator.nullsLast(Comparator.naturalOrder())));

                    // For EVENING trips, reverse the stop order logically
                    if (direction == TripDirection.EVENING) {
                        Collections.reverse(stops);
                    }

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

        // Retrieve sourceType from the stored BusLocation
        String sourceType = (loc != null) ? loc.getSourceType() : null;

        LiveBusStatusResponse resp = new LiveBusStatusResponse(
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
                activeTrip.map(Trip::isPassengerRequestActive).orElse(false),
                direction.name(),
                startingPoint,
                destination,
                sourceType
        );
        return resp;
    }

    @Transactional
    public BusLocation updateBusLocation(String busId, LocationRequest request, User currentUser) {
        Bus bus = busRepository.findById(busId)
                .orElseGet(() -> busRepository.findByBusNumber(busId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bus not found: " + busId)));

        BusLocation busLocation = busLocationRepository.findByBusId(bus.getId())
                .orElseGet(() -> {
                    BusLocation loc = new BusLocation();
                    loc.setBusId(bus.getId());
                    return loc;
                });

        String callerSourceType;

        if (currentUser.getRole() == Role.ADMIN) {
            // ADMIN: always allowed
            callerSourceType = "ADMIN";
        } else if (currentUser.getRole() == Role.DRIVER || currentUser.getRole() == Role.TRACKER) {
            // DRIVER: must be the assigned tracker for this bus
            if (bus.getTrackerId() == null || !bus.getTrackerId().equals(currentUser.getId())) {
                throw new BadRequestException("You are not authorized to update coordinates for this bus");
            }
            callerSourceType = "DRIVER";
        } else if (currentUser.getRole() == Role.STUDENT) {
            // STUDENT: full authorization check
            validateStudentLocationShare(bus, busLocation, currentUser);
            callerSourceType = "STUDENT";
        } else {
            throw new BadRequestException("You are not authorized to update bus location");
        }

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
        busLocation.setSourceType(callerSourceType);
        busLocation.setSourceUserId(currentUser.getId());

        if (bus.getStatus() == BusStatus.NOT_STARTED || bus.getStatus() == BusStatus.OFFLINE) {
            bus.setStatus(BusStatus.RUNNING);
            busRepository.save(bus);
        }

        BusLocation savedLocation = busLocationRepository.save(busLocation);

        // Update active trip location if present and check arrival reminders
        Optional<Trip> activeTripOpt = tripRepository.findFirstByBusIdAndStatus(bus.getId(), TripStatus.ACTIVE);
        if (activeTripOpt.isPresent()) {
            Trip trip = activeTripOpt.get();
            trip.setLastLocation(point);
            trip.setLastSpeed(request.getSpeed());
            trip.setLastAccuracy(request.getAccuracy());
            trip.setLastHeading(request.getHeading());
            trip.setLastUpdated(now);
            tripRepository.save(trip);

            // Automated student arrival reminder calculation
            checkAndTriggerArrivalReminders(trip, bus, request.getLatitude(), request.getLongitude(), calculatedSpeed);
        }

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

    /**
     * Validates that a student is allowed to share location for the given bus.
     * Throws BadRequestException if not authorized.
     * Priority: DRIVER > ADMIN > STUDENT.
     */
    private void validateStudentLocationShare(Bus bus, BusLocation busLocation, User student) {
        // 1. Bus must have an active trip
        Optional<Trip> activeTrip = tripRepository.findFirstByBusIdAndStatus(bus.getId(), TripStatus.ACTIVE);
        if (activeTrip.isEmpty()) {
            throw new BadRequestException("No active trip for this bus. Cannot share location.");
        }

        // 2. Student must be assigned to this bus
        Optional<Student> studentProfile = studentRepository.findByUserId(student.getId());
        if (studentProfile.isEmpty()) {
            throw new BadRequestException("Student profile not found.");
        }
        Student s = studentProfile.get();
        boolean assignedToThisBus = bus.getId().equals(s.getAssignedBus())
                || bus.getBusNumber().equals(s.getAssignedBus());
        if (!assignedToThisBus) {
            throw new BadRequestException("You are not assigned to this bus.");
        }

        // 3. Check that no higher-priority source is currently active (within last 60 seconds)
        if (busLocation.getUpdatedAt() != null) {
            long secondsOld = Duration.between(busLocation.getUpdatedAt(), Instant.now()).getSeconds();
            if (secondsOld <= 60) {
                String existingSource = busLocation.getSourceType();
                if ("DRIVER".equals(existingSource)) {
                    throw new BadRequestException("Driver is currently sharing the bus location. You cannot override the driver's GPS.");
                }
                if ("ADMIN".equals(existingSource)) {
                    throw new BadRequestException("An admin is currently managing the bus location.");
                }
                // Another student is sharing
                if ("STUDENT".equals(existingSource)
                        && busLocation.getSourceUserId() != null
                        && !busLocation.getSourceUserId().equals(student.getId())) {
                    throw new BadRequestException("Bus location is already being shared by another passenger.");
                }
            }
        }
    }

    /**
     * Clears the student as the active location source if they are the current source.
     * Called when a student explicitly clicks "Stop Sharing".
     */
    @Transactional
    public void clearStudentLocationSource(String busId, User student) {
        Bus bus = busRepository.findById(busId)
                .orElseGet(() -> busRepository.findByBusNumber(busId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bus not found: " + busId)));

        busLocationRepository.findByBusId(bus.getId()).ifPresent(loc -> {
            if ("STUDENT".equals(loc.getSourceType())
                    && student.getId().equals(loc.getSourceUserId())) {
                loc.setSourceType(null);
                loc.setSourceUserId(null);
                busLocationRepository.save(loc);
            }
        });
    }

    /**
     * Returns the current location source info for a bus.
     * Returns a map with: sourceType (DRIVER/ADMIN/STUDENT/null), canStudentShare (boolean).
     */
    public Map<String, Object> getLocationSourceStatus(String busId, User requestingUser) {
        Bus bus = busRepository.findById(busId)
                .orElseGet(() -> busRepository.findByBusNumber(busId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bus not found: " + busId)));

        Optional<Trip> activeTrip = tripRepository.findFirstByBusIdAndStatus(bus.getId(), TripStatus.ACTIVE);
        BusLocation loc = busLocationRepository.findByBusId(bus.getId()).orElse(null);

        String currentSource = null;
        boolean isStudentCurrentSource = false;

        if (loc != null && loc.getUpdatedAt() != null) {
            long secondsOld = Duration.between(loc.getUpdatedAt(), Instant.now()).getSeconds();
            if (secondsOld <= 60) {
                currentSource = loc.getSourceType();
                isStudentCurrentSource = "STUDENT".equals(currentSource)
                        && requestingUser.getId().equals(loc.getSourceUserId());
            }
        }

        boolean canShare = false;
        if (activeTrip.isPresent() && requestingUser.getRole() == Role.STUDENT) {
            Optional<Student> sp = studentRepository.findByUserId(requestingUser.getId());
            if (sp.isPresent()) {
                Student s = sp.get();
                boolean assigned = bus.getId().equals(s.getAssignedBus()) || bus.getBusNumber().equals(s.getAssignedBus());
                boolean driverBlocking = "DRIVER".equals(currentSource) || "ADMIN".equals(currentSource);
                boolean otherStudentBlocking = "STUDENT".equals(currentSource)
                        && !requestingUser.getId().equals(loc != null ? loc.getSourceUserId() : null);
                canShare = assigned && !driverBlocking && !otherStudentBlocking;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("busId", bus.getId());
        result.put("busNumber", bus.getBusNumber());
        result.put("activeTripExists", activeTrip.isPresent());
        result.put("currentSource", currentSource);
        result.put("isCurrentSource", isStudentCurrentSource);
        result.put("canStudentShare", canShare);
        return result;
    }

    private void checkAndTriggerArrivalReminders(Trip trip, Bus bus, double currentLat, double currentLng, double currentSpeed) {
        if (trip == null || trip.getId() == null) return;

        List<Student> assignedStudents = studentRepository.findByAssignedBus(bus.getId());
        if (assignedStudents.isEmpty() && bus.getRouteId() != null) {
            assignedStudents = studentRepository.findByAssignedRoute(bus.getRouteId());
        }
        if (assignedStudents.isEmpty()) return;

        double effectiveSpeed = (currentSpeed > 5.0) ? currentSpeed : 28.0; // km/h
        TripDirection direction = trip.getDirection() != null ? trip.getDirection() : TripDirection.MORNING;

        for (Student s : assignedStudents) {
            if (trip.getRemindedStudentIds() != null && trip.getRemindedStudentIds().contains(s.getId())) {
                continue;
            }

            // Target stop location: For evening, use eveningDropLocation or fallback to boardingLocation
            GeoPoint targetLoc = (direction == TripDirection.EVENING && s.getEveningDropLocation() != null)
                    ? s.getEveningDropLocation()
                    : s.getBoardingLocation();

            if (targetLoc == null || targetLoc.getCoordinates() == null || targetLoc.getCoordinates().size() < 2) {
                continue;
            }

            double targetLng = targetLoc.getCoordinates().get(0);
            double targetLat = targetLoc.getCoordinates().get(1);

            double distMeters = calculateDistanceMeters(currentLat, currentLng, targetLat, targetLng);
            double distKm = distMeters / 1000.0;
            double etaMin = (distKm / effectiveSpeed) * 60.0;

            int threshold = (s.getReminderMinutes() != null && s.getReminderMinutes() > 0) ? s.getReminderMinutes() : 10;

            // Check if ETA is within reminder threshold and within 15km corridor
            if (etaMin <= threshold && distKm <= 15.0) {
                // Check if student marked NOT_ON_BUS for this trip
                if (trip.getPassengerConfirmations() != null) {
                    boolean markedNotOnBus = trip.getPassengerConfirmations().stream()
                            .anyMatch(p -> s.getId().equals(p.getStudentId()) && p.getStatus() == PassengerStatus.NOT_ON_BUS);
                    if (markedNotOnBus) continue;
                }

                // Mark as reminded on trip
                if (trip.getRemindedStudentIds() == null) {
                    trip.setRemindedStudentIds(new HashSet<>());
                }
                trip.getRemindedStudentIds().add(s.getId());
                tripRepository.save(trip);

                String stopName = (direction == TripDirection.EVENING && s.getEveningDropAddress() != null)
                        ? s.getEveningDropAddress()
                        : "your stop";

                int roundedEta = Math.max(1, (int) Math.round(etaMin));
                String reminderMsg = bus.getBusNumber() + " is approximately " + roundedEta + " minutes away from " + stopName + ". Prepare to board.";

                TripReminder reminder = new TripReminder();
                reminder.setTripId(trip.getId());
                reminder.setStudentId(s.getId());
                reminder.setStudentName(s.getFullName());
                reminder.setBusNumber(bus.getBusNumber());
                reminder.setStopName(stopName);
                reminder.setEtaMinutes((double) roundedEta);
                reminder.setMessage(reminderMsg);
                reminder.setSentAt(Instant.now());
                tripReminderRepository.save(reminder);

                // Broadcast reminder over WebSocket
                Map<String, Object> reminderPayload = new HashMap<>();
                reminderPayload.put("type", "ARRIVAL_REMINDER");
                reminderPayload.put("tripId", trip.getId());
                reminderPayload.put("direction", direction.name());
                reminderPayload.put("studentId", s.getId());
                reminderPayload.put("busNumber", bus.getBusNumber());
                reminderPayload.put("etaMinutes", roundedEta);
                reminderPayload.put("stopName", stopName);
                reminderPayload.put("message", reminderMsg);
                reminderPayload.put("timestamp", Instant.now().toString());

                try {
                    messagingTemplate.convertAndSend("/topic/bus/" + bus.getBusNumber() + "/reminders", reminderPayload);
                    messagingTemplate.convertAndSend("/topic/trip/" + trip.getId() + "/reminders", reminderPayload);
                    messagingTemplate.convertAndSend("/topic/student/" + s.getId() + "/reminders", reminderPayload);
                } catch (Exception e) {
                    System.err.println("Reminder broadcast error: " + e.getMessage());
                }
            }
        }
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
