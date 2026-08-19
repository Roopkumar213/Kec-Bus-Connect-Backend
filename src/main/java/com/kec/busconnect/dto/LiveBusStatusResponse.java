package com.kec.busconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveBusStatusResponse {
    private String busId;
    private String busNumber;
    private String registrationNumber;
    private String status; // RUNNING, COMPLETED, NOT_STARTED, etc.
    private String freshness; // LIVE, STALE, LOCATION_DELAYED
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private Double speed; // in km/h
    private Double heading;
    private Instant lastUpdated;
    private Long secondsSinceLastUpdate;
    private String currentlyAtStop; // e.g., "KANGUNDHI" or null if in transit
    private String previousStop;
    private String nextStop;
    private Double distanceToNextStopKm;
    private Double etaMinutesToNextStop;
    private String activeTripId;
    private boolean passengerRequestActive;
    private String direction; // MORNING, EVENING
    private String startingPoint;
    private String destination;
}
