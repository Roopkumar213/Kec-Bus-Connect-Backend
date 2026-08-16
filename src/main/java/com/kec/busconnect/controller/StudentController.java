package com.kec.busconnect.controller;

import com.kec.busconnect.model.Student;
import com.kec.busconnect.security.UserPrincipal;
import com.kec.busconnect.service.LocationService;
import com.kec.busconnect.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final LocationService locationService;

    public StudentController(StudentService studentService, LocationService locationService) {
        this.studentService = studentService;
        this.locationService = locationService;
    }

    @GetMapping("/me")
    public ResponseEntity<Student> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        Student student = studentService.getProfileByUserId(principal.getUser().getId());
        return ResponseEntity.ok(student);
    }

    @PutMapping("/me")
    public ResponseEntity<Student> updateMyProfile(@AuthenticationPrincipal UserPrincipal principal, @RequestBody Student updatedFields) {
        Student student = studentService.updateProfile(principal.getUser().getId(), updatedFields);
        return ResponseEntity.ok(student);
    }

    @PostMapping("/board/{busId}")
    public ResponseEntity<Map<String, Object>> confirmBoarding(
            @PathVariable String busId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Student student = studentService.getProfileByUserId(principal.getUser().getId());
        boolean boarded = locationService.recordStudentBoarding(busId, student.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("boarded", boarded);
        response.put("message", "Boarding status confirmed for bus " + busId);
        return ResponseEntity.ok(response);
    }
}
