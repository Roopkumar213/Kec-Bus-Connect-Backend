package com.kec.busconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "routes")
public class Route {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String name;
    
    private PointName startPoint;
    private PointName destination;
    
    private List<Stop> stops;
    
    private boolean isActive = true;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointName {
        private String name;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stop {
        private String name;
        private Integer sequence;
        private GeoPoint location;
    }
}
