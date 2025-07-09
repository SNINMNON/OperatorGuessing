package com.sninmnon.demo.entity;

import lombok.Data;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Data
public class WebSocketMessage {
    private String type;      // join, ready, guess, etc.
    private String userId;
    private final Map<String, Object> data = new HashMap<>();

    public WebSocketMessage() {}

    public WebSocketMessage(String type) {
        this.type = type;
    }

    public String getData(String key) {
        return (String) this.data.get(key);
    }

    public void putData(String key, Object value) {
        this.data.put(key, value);
    }
}
