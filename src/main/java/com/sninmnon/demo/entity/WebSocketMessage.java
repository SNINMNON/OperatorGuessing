package com.sninmnon.demo.entity;

import lombok.Data;

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

    public String getDataStr(String key) {
        return (String) this.data.get(key);
    }

    public Boolean getDataBool(String key) {
        Object val = data.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof String) {
            return Boolean.parseBoolean((String) val);
        }
        return null;
    }

    public void putData(String key, Object value) {
        this.data.put(key, value);
    }
}
