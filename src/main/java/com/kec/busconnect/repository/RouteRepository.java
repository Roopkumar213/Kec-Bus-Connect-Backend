package com.kec.busconnect.repository;

import com.kec.busconnect.model.Route;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends MongoRepository<Route, String> {
    Optional<Route> findByName(String name);
    boolean existsByName(String name);
    List<Route> findByIsActive(boolean isActive);
}
