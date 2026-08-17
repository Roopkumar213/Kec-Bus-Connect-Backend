package com.kec.busconnect.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BoardingLocationRequest {
    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private Double accuracy;

    private String addressName;
}
