package com.kec.busconnect.controller;

import com.kec.busconnect.dto.AuthResponse;
import com.kec.busconnect.dto.LoginRequest;
import com.kec.busconnect.dto.MeResponse;
import com.kec.busconnect.dto.SignupRequest;
import com.kec.busconnect.enums.Role;
import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.Route;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.model.User;
import com.kec.busconnect.repository.BusRepository;
import com.kec.busconnect.repository.RouteRepository;
import com.kec.busconnect.security.UserPrincipal;
import com.kec.busconnect.service.AuthService;
import com.kec.busconnect.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final StudentService studentService;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;

    public AuthController(AuthService authService,
                          StudentService studentService,
                          BusRepository busRepository,
                          RouteRepository routeRepository) {
        this.authService = authService;
        this.studentService = studentService;
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
    }

    @PostMapping("/student/signup")
    public ResponseEntity<Map<String, Object>> studentSignup(@Valid @RequestBody SignupRequest request) {
        authService.registerStudent(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Student registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        Student student = null;
        Bus assignedBus = null;
        Route assignedRoute = null;

        if (user.getRole() == Role.STUDENT) {
            try {
                student = studentService.getProfileByUserId(user.getId());
                if (student.getAssignedBus() != null) {
                    assignedBus = busRepository.findById(student.getAssignedBus()).orElse(null);
                }
                if (student.getAssignedRoute() != null) {
                    assignedRoute = routeRepository.findById(student.getAssignedRoute()).orElse(null);
                }
            } catch (Exception e) {
                // Ignore missing profiles
            }
        }

        return ResponseEntity.ok(new MeResponse(user, student, assignedBus, assignedRoute));
    }
}
