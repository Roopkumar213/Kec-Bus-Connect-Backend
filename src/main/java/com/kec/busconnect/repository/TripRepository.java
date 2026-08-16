package com.kec.busconnect.repository;

import com.kec.busconnect.enums.TripStatus;
import com.kec.busconnect.model.Trip;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends MongoRepository<Trip, String> {
    Optional<Trip> findFirstByBusIdAndStatus(String busId, TripStatus status);
    Optional<Trip> findFirstByDriverIdAndStatus(String driverId, TripStatus status);
    List<Trip> findByBusIdOrderByStartTimeDesc(String busId);
    List<Trip> findByStatus(TripStatus status);
}
