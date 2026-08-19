package com.kec.busconnect.repository;

import com.kec.busconnect.model.TripReminder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripReminderRepository extends MongoRepository<TripReminder, String> {
    List<TripReminder> findByTripId(String tripId);
    List<TripReminder> findByStudentId(String studentId);
    Optional<TripReminder> findByTripIdAndStudentId(String tripId, String studentId);
    boolean existsByTripIdAndStudentId(String tripId, String studentId);
}
