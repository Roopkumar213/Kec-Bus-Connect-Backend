package com.kec.busconnect.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "buslocations")
public class BusLocation {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String busId;
    
    @GeoSpatialIndexed
    private GeoPoint location;
    
    private Double accuracy;
    private Double speed;
    private Double heading;
    
    @LastModifiedDate
    private Instant updatedAt;
}
