package com.kec.busconnect.model;

import com.kec.busconnect.enums.TripStatus;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Document(collection = "trips")
public class Trip {
    @Id
    private String id;

    @Indexed
    private String busId;

    private String busNumber;

    @Indexed
    private String driverId;

    private String routeId;

    private TripStatus status = TripStatus.ACTIVE;

    private Instant startTime;

    private Instant endTime;

    private Set<String> boardedStudentIds = new HashSet<>();

    private List<PassengerConfirmation> passengerConfirmations = new ArrayList<>();

    private boolean passengerRequestActive = false;

    private Instant passengerRequestTimestamp;

    private GeoPoint lastLocation;

    private Double lastSpeed;

    private Double lastAccuracy;

    private Double lastHeading;

    private Instant lastUpdated;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
