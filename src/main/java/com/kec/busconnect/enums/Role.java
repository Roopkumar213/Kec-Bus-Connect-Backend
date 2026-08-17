package com.kec.busconnect.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    STUDENT,
    DRIVER,
    ADMIN,
    @Deprecated
    TRACKER; // backward compatibility for legacy DB documents

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null) return STUDENT;
        String upper = value.trim().toUpperCase();
        if ("TRACKER".equals(upper)) {
            return DRIVER;
        }
        try {
            return Role.valueOf(upper);
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }

    @JsonValue
    public String toValue() {
        if (this == TRACKER) return DRIVER.name();
        return this.name();
    }
}
