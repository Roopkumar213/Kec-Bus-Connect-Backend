package com.kec.busconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusLocationResponse {
    private String busNumber;
    private String status;
    private LatLng location;
    private Double accuracy;
    private Double speed;
    private Double heading;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatLng {
        private Double latitude;
        private Double longitude;
    }
}
