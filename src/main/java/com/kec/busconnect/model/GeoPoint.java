package com.kec.busconnect.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {
    private String type = "Point";
    private List<Double> coordinates; // [longitude, latitude]
}
