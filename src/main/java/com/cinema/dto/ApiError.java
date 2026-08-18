package com.cinema.dto;

import java.util.Map;

public class ApiError {
    private String message;

    public ApiError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> toMap() {
        return Map.of("message", message);
    }
}
