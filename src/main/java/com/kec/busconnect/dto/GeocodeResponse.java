package com.kec.busconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeocodeResponse {
    private Double latitude;
    private Double longitude;
    private String displayName;
    private String road;
    private String villageOrLocality;
    private String townOrCity;
    private String district;
    private String state;
    private String formattedShort;
}
