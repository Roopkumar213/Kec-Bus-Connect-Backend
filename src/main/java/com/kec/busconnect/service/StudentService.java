package com.kec.busconnect.service;

import com.kec.busconnect.exception.ResourceNotFoundException;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.repository.StudentRepository;
import org.springframework.stereotype.Service;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student getProfileByUserId(String userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user ID: " + userId));
    }

    public Student updateProfile(String userId, Student updatedStudent) {
        Student student = getProfileByUserId(userId);
        
        if (updatedStudent.getFullName() != null) {
            student.setFullName(updatedStudent.getFullName().trim());
        }
        if (updatedStudent.getMobile() != null) {
            student.setMobile(updatedStudent.getMobile().trim());
        }
        if (updatedStudent.getSection() != null) {
            student.setSection(updatedStudent.getSection().trim());
        }
        if (updatedStudent.getBatch() != null) {
            student.setBatch(updatedStudent.getBatch().trim());
        }
        if (updatedStudent.getBoardingLocation() != null) {
            student.setBoardingLocation(updatedStudent.getBoardingLocation());
        }

        return studentRepository.save(student);
    }
}
