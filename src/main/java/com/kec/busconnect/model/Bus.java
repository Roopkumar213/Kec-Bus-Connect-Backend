package com.kec.busconnect.model;

import com.kec.busconnect.enums.BusStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "buses")
public class Bus {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String busNumber;
    
    private String registrationNumber;
    
    private String routeId;
    private String trackerId;
    
    private BusStatus status = BusStatus.NOT_STARTED;
    
    private boolean isActive = true;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}
