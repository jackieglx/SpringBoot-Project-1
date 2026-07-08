package com.april.studentmanagementproject.dto;

import jakarta.validation.constraints.NotBlank;

public class KafkaMessageRequest {

    @NotBlank(message = "Message key is required")
    private String key;

    @NotBlank(message = "Message value is required")
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
