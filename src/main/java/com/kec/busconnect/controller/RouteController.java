package com.kec.busconnect.controller;

import com.kec.busconnect.model.Route;
import com.kec.busconnect.service.RouteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public ResponseEntity<List<Route>> getActiveRoutes() {
        List<Route> activeRoutes = routeService.getActiveRoutes();
        return ResponseEntity.ok(activeRoutes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Route> getRouteDetails(@PathVariable String id) {
        Route route = routeService.getRouteById(id);
        return ResponseEntity.ok(route);
    }
}
