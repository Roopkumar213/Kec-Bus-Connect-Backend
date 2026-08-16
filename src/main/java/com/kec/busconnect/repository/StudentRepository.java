package com.kec.busconnect.repository;

import com.kec.busconnect.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {
    Optional<Student> findByUserId(String userId);
    Optional<Student> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
}
