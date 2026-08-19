package com.kec.busconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "trip_reminders")
@CompoundIndex(name = "trip_student_unique_idx", def = "{'tripId': 1, 'studentId': 1}", unique = true)
public class TripReminder {

    @Id
    private String id;

    @Indexed
    private String tripId;

    @Indexed
    private String studentId;

    private String studentName;

    private String busNumber;

    private String stopName;

    private Double etaMinutes;

    private String message;

    @CreatedDate
    private Instant sentAt = Instant.now();
}
