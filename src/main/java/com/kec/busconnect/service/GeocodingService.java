package com.kec.busconnect.service;

import com.kec.busconnect.dto.GeocodeResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, GeocodeResponse> cache = new ConcurrentHashMap<>();

    public GeocodeResponse reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return new GeocodeResponse(0.0, 0.0, "Unknown Location", "", "", "", "", "", "Unknown Location");
        }

        // Cache key rounded to ~10-20 meters (4 decimals)
        String cacheKey = String.format("%.4f,%.4f", latitude, longitude);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        try {
            String url = String.format(
                    "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1",
                    latitude, longitude
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "KEC-BusConnect/1.0 (Kuppam Engineering College Bus Tracking)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                String displayName = (String) body.get("display_name");
                Map address = (Map) body.get("address");

                String road = "";
                String villageOrLocality = "";
                String townOrCity = "";
                String district = "";
                String state = "";

                if (address != null) {
                    if (address.containsKey("road")) road = (String) address.get("road");
                    if (address.containsKey("village")) villageOrLocality = (String) address.get("village");
                    else if (address.containsKey("suburb")) villageOrLocality = (String) address.get("suburb");
                    else if (address.containsKey("neighbourhood")) villageOrLocality = (String) address.get("neighbourhood");
                    else if (address.containsKey("hamlet")) villageOrLocality = (String) address.get("hamlet");

                    if (address.containsKey("town")) townOrCity = (String) address.get("town");
                    else if (address.containsKey("city")) townOrCity = (String) address.get("city");
                    else if (address.containsKey("county")) townOrCity = (String) address.get("county");

                    if (address.containsKey("state_district")) district = (String) address.get("state_district");
                    else if (address.containsKey("district")) district = (String) address.get("district");

                    if (address.containsKey("state")) state = (String) address.get("state");
                }

                // Create a concise readable label
                StringBuilder shortLabel = new StringBuilder();
                if (!road.isEmpty()) shortLabel.append(road);
                if (!villageOrLocality.isEmpty()) {
                    if (shortLabel.length() > 0) shortLabel.append(", ");
                    shortLabel.append(villageOrLocality);
                }
                if (!townOrCity.isEmpty() && !townOrCity.equalsIgnoreCase(villageOrLocality)) {
                    if (shortLabel.length() > 0) shortLabel.append(", ");
                    shortLabel.append(townOrCity);
                }
                if (shortLabel.length() == 0) {
                    shortLabel.append(displayName != null && !displayName.isEmpty() ? displayName : String.format("GPS: %.4f, %.4f", latitude, longitude));
                }

                GeocodeResponse result = new GeocodeResponse(
                        latitude, longitude, displayName, road, villageOrLocality, townOrCity, district, state, shortLabel.toString()
                );
                cache.put(cacheKey, result);
                return result;
            }
        } catch (Exception e) {
            System.err.println("Reverse geocoding query failed: " + e.getMessage());
        }

        // Fallback if external service fails or network is offline
        String fallbackShort = String.format("GPS: %.4f, %.4f", latitude, longitude);
        GeocodeResponse fallback = new GeocodeResponse(
                latitude, longitude, fallbackShort, "", "", "Kuppam Area", "Chittoor", "Andhra Pradesh", fallbackShort
        );
        cache.put(cacheKey, fallback);
        return fallback;
    }
}
