package com.kec.busconnect.repository;

import com.kec.busconnect.model.BusLocation;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface BusLocationRepository extends MongoRepository<BusLocation, String> {
    Optional<BusLocation> findByBusId(String busId);
    boolean existsByBusId(String busId);
}
