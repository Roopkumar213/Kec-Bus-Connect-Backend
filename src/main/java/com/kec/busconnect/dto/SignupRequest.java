package com.kec.busconnect.dto;

import com.kec.busconnect.enums.CollegeType;
import com.kec.busconnect.enums.Program;
import com.kec.busconnect.enums.Department;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please enter a valid 10-digit mobile number starting with 6-9")
    private String mobile;

    @NotNull(message = "College type is required")
    private CollegeType collegeType;

    @NotNull(message = "Program is required")
    private Program program;

    private Department department;

    @NotNull(message = "Academic year is required")
    @Min(value = 1, message = "Academic year must be at least 1")
    @Max(value = 4, message = "Academic year must be at most 4")
    private Integer academicYear;

    private String section;
    private String batch;

    @NotNull(message = "Boarding location is required")
    private BoardingLocationDto boardingLocation;

    private String assignedBus;
    private String assignedRoute;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @Data
    public static class BoardingLocationDto {
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        private Double latitude;

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        private Double longitude;

        private Double accuracy;
    }
}
