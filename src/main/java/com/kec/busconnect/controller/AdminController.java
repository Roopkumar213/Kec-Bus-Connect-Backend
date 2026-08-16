package com.kec.busconnect.controller;

import com.kec.busconnect.model.Bus;
import com.kec.busconnect.model.Route;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.repository.StudentRepository;
import com.kec.busconnect.service.BusService;
import com.kec.busconnect.service.RouteService;
import com.kec.busconnect.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final BusService busService;
    private final RouteService routeService;
    private final StudentRepository studentRepository;

    public AdminController(BusService busService,
                           RouteService routeService,
                           StudentRepository studentRepository) {
        this.busService = busService;
        this.routeService = routeService;
        this.studentRepository = studentRepository;
    }

    @PostMapping("/buses")
    public ResponseEntity<Bus> createBus(@Valid @RequestBody Bus bus) {
        Bus created = busService.createBus(bus);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/buses")
    public ResponseEntity<List<Bus>> getAllBuses() {
        List<Bus> buses = busService.getAllBuses();
        return ResponseEntity.ok(buses);
    }

    @PutMapping("/buses/{id}")
    public ResponseEntity<Bus> updateBus(@PathVariable String id, @Valid @RequestBody Bus bus) {
        Bus updated = busService.updateBus(id, bus);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/buses/{id}")
    public ResponseEntity<Void> deleteBus(@PathVariable String id) {
        busService.deleteBus(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/routes")
    public ResponseEntity<Route> createRoute(@Valid @RequestBody Route route) {
        Route created = routeService.createRoute(route);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/routes")
    public ResponseEntity<List<Route>> getAllRoutes() {
        List<Route> routes = routeService.getAllRoutes();
        return ResponseEntity.ok(routes);
    }

    @PutMapping("/routes/{id}")
    public ResponseEntity<Route> updateRoute(@PathVariable String id, @Valid @RequestBody Route route) {
        Route updated = routeService.updateRoute(id, route);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/routes/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable String id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/students")
    public ResponseEntity<List<Student>> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
        return ResponseEntity.ok(student);
    }
}
