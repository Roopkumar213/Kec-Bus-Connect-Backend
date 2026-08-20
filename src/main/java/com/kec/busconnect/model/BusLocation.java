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

    /**
     * Who is currently providing this bus's live location.
     * Values: "DRIVER", "ADMIN", "STUDENT"
     * Null means legacy/unknown source (treated as DRIVER for priority purposes).
     */
    private String sourceType;

    /**
     * The userId of the current location source (User document ID).
     * Used to identify which student is sharing when sourceType = "STUDENT".
     * Not exposed publicly — frontend only sees sourceType.
     */
    private String sourceUserId;
    
    @LastModifiedDate
    private Instant updatedAt;
}
