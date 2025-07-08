package com.sninmnon.demo.entity;

import lombok.Data;

import java.util.Map;

@Data
public class WebSocketMessage {
    private String type;      // join, ready, guess, etc.
    private String roomId;
    private String userId;
    private String guess;
    private Object payload; // gOperator{}, comparison{}

    public WebSocketMessage() {}

    public WebSocketMessage(String type, String roomId, String userId, Object payload) {
        this.type = type;
        this.roomId = roomId;
        this.userId = userId;
        this.payload = payload;
    }

}
