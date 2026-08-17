package com.kec.busconnect.dto;

import com.kec.busconnect.model.PassengerConfirmation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerSummaryResponse {
    private String tripId;
    private String busNumber;
    private int confirmedCount;
    private int notRespondedCount;
    private int notOnBusCount;
    private int totalAssigned;
    private boolean isRequestActive;
    private List<PassengerConfirmation> passengers;
}
