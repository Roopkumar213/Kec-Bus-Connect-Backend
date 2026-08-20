package com.kec.busconnect.controller;

import com.kec.busconnect.dto.BoardingLocationRequest;
import com.kec.busconnect.enums.PassengerStatus;
import com.kec.busconnect.model.PassengerConfirmation;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.security.UserPrincipal;
import com.kec.busconnect.service.LocationService;
import com.kec.busconnect.service.StudentService;
import com.kec.busconnect.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class StudentController {

    private final StudentService studentService;
    private final LocationService locationService;
    private final TripService tripService;

    public StudentController(StudentService studentService, LocationService locationService, TripService tripService) {
        this.studentService = studentService;
        this.locationService = locationService;
        this.tripService = tripService;
    }

    @GetMapping("/api/students/me")
    public ResponseEntity<Student> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        Student student = studentService.getProfileByUserId(principal.getUser().getId());
        return ResponseEntity.ok(student);
    }

    @PutMapping("/api/students/me")
    public ResponseEntity<Student> updateMyProfile(@AuthenticationPrincipal UserPrincipal principal, @RequestBody Student updatedFields) {
        Student student = studentService.updateProfile(principal.getUser().getId(), updatedFields);
        return ResponseEntity.ok(student);
    }

    @GetMapping("/api/student/boarding-location")
    public ResponseEntity<Map<String, Object>> getBoardingLocation(@AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> location = studentService.getBoardingLocation(principal.getUser().getId());
        return ResponseEntity.ok(location);
    }

    @PutMapping("/api/student/boarding-location")
    public ResponseEntity<Student> updateBoardingLocation(
            @Valid @RequestBody BoardingLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Student updated = studentService.updateBoardingLocation(principal.getUser().getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/api/student/boarding-location")
    public ResponseEntity<Student> saveBoardingLocation(
            @Valid @RequestBody BoardingLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Student updated = studentService.updateBoardingLocation(principal.getUser().getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/api/student/evening-drop-location")
    public ResponseEntity<Student> updateEveningDropLocation(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Double lat = body.get("latitude") != null ? Double.valueOf(body.get("latitude").toString()) : null;
        Double lng = body.get("longitude") != null ? Double.valueOf(body.get("longitude").toString()) : null;
        Double acc = body.get("accuracy") != null ? Double.valueOf(body.get("accuracy").toString()) : 10.0;
        String address = body.get("addressName") != null ? body.get("addressName").toString() : null;

        if (lat == null || lng == null) {
            return ResponseEntity.badRequest().build();
        }

        BoardingLocationRequest req = new BoardingLocationRequest();
        req.setLatitude(lat);
        req.setLongitude(lng);
        req.setAccuracy(acc);

        Student updated = studentService.updateEveningDropLocation(principal.getUser().getId(), req, address);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/api/student/reminder-settings")
    public ResponseEntity<Student> updateReminderSettings(
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        int minutes = body.getOrDefault("reminderMinutes", 10);
        Student updated = studentService.updateReminderMinutes(principal.getUser().getId(), minutes);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/api/student/trips/{tripId}/confirm")
    public ResponseEntity<PassengerConfirmation> confirmOnBus(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PassengerConfirmation confirmation = tripService.updatePassengerStatus(
                tripId,
                principal.getUser().getId(),
                PassengerStatus.CONFIRMED_ON_BUS
        );
        return ResponseEntity.ok(confirmation);
    }

    @PostMapping("/api/student/trips/{tripId}/not-on-bus")
    public ResponseEntity<PassengerConfirmation> notOnBus(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PassengerConfirmation confirmation = tripService.updatePassengerStatus(
                tripId,
                principal.getUser().getId(),
                PassengerStatus.NOT_ON_BUS
        );
        return ResponseEntity.ok(confirmation);
    }

    @PostMapping("/api/students/board/{busId}")
    public ResponseEntity<Map<String, Object>> legacyConfirmBoarding(
            @PathVariable String busId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Student student = studentService.getProfileByUserId(principal.getUser().getId());
        boolean boarded = locationService.getActiveTripForBus(busId).isPresent();
        if (boarded) {
            tripService.getActiveTripForBus(busId).ifPresent(trip -> {
                tripService.updatePassengerStatus(trip.getId(), principal.getUser().getId(), PassengerStatus.CONFIRMED_ON_BUS);
            });
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("boarded", boarded);
        response.put("message", "Boarding status recorded for bus " + busId);
        return ResponseEntity.ok(response);
    }

    // =========================================================
    // Student Live Location Sharing endpoints
    // =========================================================

    /**
     * GET /api/student/location/status?busId={busId}
     * Returns whether the student can share location for the specified bus,
     * and who the current location source is.
     */
    @GetMapping("/api/student/location/status")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getLocationShareStatus(
            @RequestParam String busId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Map<String, Object> status = locationService.getLocationSourceStatus(busId, principal.getUser());
        return ResponseEntity.ok(status);
    }

    /**
     * POST /api/student/location/stop?busId={busId}
     * Clears the student as the active location source for the bus.
     * Safe to call even if the student is not currently sharing.
     */
    @PostMapping("/api/student/location/stop")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> stopLocationSharing(
            @RequestParam String busId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        locationService.clearStudentLocationSource(busId, principal.getUser());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Location sharing stopped.");
        return ResponseEntity.ok(response);
    }
}

