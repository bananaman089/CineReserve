package com.cinema.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProjectionFormat {
    TWO_D("2D"),
    THREE_D("3D");

    private final String label;

    ProjectionFormat(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static ProjectionFormat from(String value) {
        if (value == null) {
            return TWO_D;
        }
        for (ProjectionFormat format : values()) {
            if (format.label.equalsIgnoreCase(value) || format.name().equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Невалиден формат: " + value);
    }
}
