package com.kec.busconnect.model;

import com.kec.busconnect.enums.PassengerStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerConfirmation {
    private String studentId;
    private String studentName;
    private String studentRollNumber;
    private String boardingStopName;
    private PassengerStatus status = PassengerStatus.PENDING;
    private Instant confirmedAt;
}
