package com.kec.busconnect.service;

import com.kec.busconnect.dto.BoardingLocationRequest;
import com.kec.busconnect.exception.ResourceNotFoundException;
import com.kec.busconnect.model.GeoPoint;
import com.kec.busconnect.model.Student;
import com.kec.busconnect.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    public Student updateBoardingLocation(String userId, BoardingLocationRequest request) {
        Student student = getProfileByUserId(userId);
        
        // MongoDB GeoJSON Point: [longitude, latitude]
        GeoPoint point = new GeoPoint(
                "Point",
                Arrays.asList(request.getLongitude(), request.getLatitude())
        );
        student.setBoardingLocation(point);
        return studentRepository.save(student);
    }

    public Student updateEveningDropLocation(String userId, BoardingLocationRequest request, String addressName) {
        Student student = getProfileByUserId(userId);
        
        GeoPoint point = new GeoPoint(
                "Point",
                Arrays.asList(request.getLongitude(), request.getLatitude())
        );
        student.setEveningDropLocation(point);
        if (addressName != null && !addressName.trim().isEmpty()) {
            student.setEveningDropAddress(addressName.trim());
        }
        return studentRepository.save(student);
    }

    public Student updateReminderMinutes(String userId, int minutes) {
        Student student = getProfileByUserId(userId);
        student.setReminderMinutes(Math.max(1, Math.min(30, minutes)));
        return studentRepository.save(student);
    }

    public Map<String, Object> getBoardingLocation(String userId) {
        Student student = getProfileByUserId(userId);
        Map<String, Object> map = new HashMap<>();
        if (student.getBoardingLocation() != null && student.getBoardingLocation().getCoordinates() != null && student.getBoardingLocation().getCoordinates().size() >= 2) {
            map.put("longitude", student.getBoardingLocation().getCoordinates().get(0));
            map.put("latitude", student.getBoardingLocation().getCoordinates().get(1));
        }
        if (student.getEveningDropLocation() != null && student.getEveningDropLocation().getCoordinates() != null && student.getEveningDropLocation().getCoordinates().size() >= 2) {
            map.put("eveningLongitude", student.getEveningDropLocation().getCoordinates().get(0));
            map.put("eveningLatitude", student.getEveningDropLocation().getCoordinates().get(1));
            map.put("eveningDropAddress", student.getEveningDropAddress());
        }
        map.put("reminderMinutes", student.getReminderMinutes() != null ? student.getReminderMinutes() : 10);
        map.put("assignedBus", student.getAssignedBus());
        map.put("assignedRoute", student.getAssignedRoute());
        return map;
    }
}
