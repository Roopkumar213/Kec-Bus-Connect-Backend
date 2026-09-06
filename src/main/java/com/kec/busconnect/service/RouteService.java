package com.kec.busconnect.service;

import com.kec.busconnect.exception.BadRequestException;
import com.kec.busconnect.exception.ResourceNotFoundException;
import com.kec.busconnect.model.Route;
import com.kec.busconnect.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    public List<Route> getActiveRoutes() {
        return routeRepository.findByIsActive(true);
    }

    public Route getRouteById(String id) {
        return routeRepository.findById(id)
                .or(() -> routeRepository.findByName(id))
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with ID or name: " + id));
    }

    public Route createRoute(Route route) {
        if (routeRepository.existsByName(route.getName())) {
            throw new BadRequestException("Route name already exists");
        }
        return routeRepository.save(route);
    }

    public Route updateRoute(String id, Route updatedRoute) {
        Route route = getRouteById(id);

        if (updatedRoute.getName() != null && !updatedRoute.getName().equals(route.getName())) {
            if (routeRepository.existsByName(updatedRoute.getName())) {
                throw new BadRequestException("Route name already exists");
            }
            route.setName(updatedRoute.getName());
        }

        if (updatedRoute.getStartPoint() != null) {
            route.setStartPoint(updatedRoute.getStartPoint());
        }
        if (updatedRoute.getDestination() != null) {
            route.setDestination(updatedRoute.getDestination());
        }
        if (updatedRoute.getStops() != null) {
            route.setStops(updatedRoute.getStops());
        }
        route.setActive(updatedRoute.isActive());

        return routeRepository.save(route);
    }

    public void deleteRoute(String id) {
        Route route = getRouteById(id);
        routeRepository.delete(route);
    }
}
