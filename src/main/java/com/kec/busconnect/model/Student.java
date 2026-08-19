package com.kec.busconnect.model;

import com.kec.busconnect.enums.CollegeType;
import com.kec.busconnect.enums.Program;
import com.kec.busconnect.enums.Department;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "students")
public class Student {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    private String fullName;
    
    @Indexed(unique = true)
    private String studentId;
    
    private String mobile;
    
    private CollegeType collegeType;
    private Program program;
    private Department department;
    private Integer academicYear;
    private String section;
    private String batch;
    
    @GeoSpatialIndexed
    private GeoPoint boardingLocation;
    
    @GeoSpatialIndexed
    private GeoPoint eveningDropLocation;
    
    private String eveningDropAddress;
    
    private Integer reminderMinutes = 10;
    
    private String assignedRoute;
    private String assignedBus;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
