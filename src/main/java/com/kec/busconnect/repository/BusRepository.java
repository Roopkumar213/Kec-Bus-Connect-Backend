package com.kec.busconnect.repository;

import com.kec.busconnect.model.Bus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface BusRepository extends MongoRepository<Bus, String> {
    Optional<Bus> findByBusNumber(String busNumber);
    boolean existsByBusNumber(String busNumber);
    List<Bus> findByIsActive(boolean isActive);
    Optional<Bus> findByTrackerId(String trackerId);
}
