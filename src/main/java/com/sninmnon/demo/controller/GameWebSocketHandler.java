package com.sninmnon.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sninmnon.demo.service.RoomService;
import com.sninmnon.demo.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    // roomId -> (userId -> session)
    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // userId -> roomId
    private final Map<String, String> userRoomMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RoomService roomService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: " + session.getId());
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws IOException {
        WebSocketMessage msg = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
        String userId = msg.getUserId(); // always non-null
        String roomId = msg.getRoomId(); // can be null when create/join

        log.info("Message received: type={}, user={}", msg.getType(), msg.getUserId());

        switch (msg.getType()) {
            // create room and join
            case "create":
                if (userRoomMap.get(userId) != null) {
                    sendSimpleMsg(session, "user already in room " + userRoomMap.get(userId));
                    break;
                }
                String newRoomId = roomService.createRoom(userId);
                roomSessions.put(newRoomId, new ConcurrentHashMap<>());
                roomSessions.get(newRoomId).put(userId, session);
                roomService.joinRoom(newRoomId, userId);
                userRoomMap.put(userId, newRoomId);
                broadcast(newRoomId, "create", userId, "user created and joined game room");
                break;

            // join existing room
            case "join":
                if (userRoomMap.get(userId) != null) {
                    sendSimpleMsg(session, "user already in room " + userRoomMap.get(userId));
                    break;
                } else if (roomService.roomNotExist(roomId)) {
                    sendSimpleMsg(session, "room not exist");
                    break;
                } else if (roomService.roomStarted(roomId)) {
                    sendSimpleMsg(session, "room already started");
                    break;
                }

                roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
                roomService.joinRoom(roomId, userId);
                userRoomMap.put(userId, roomId);
                broadcast(roomId, "join", userId, "user joined game room");
                break;

            // mark ready for user in game room
            case "ready":
                if (roomService.roomNotExist(roomId)) {
                    sendSimpleMsg(session, "room not exist");
                    break;
                } else if (roomService.roomStarted(roomId)) {
                    sendSimpleMsg(session, "room already started");
                    break;
                }

                roomService.markReady(roomId, userId);
                broadcast(roomId, "ready", userId, "user ready");

                if (roomService.getGameRoom(roomId).allReady()) {
                    roomService.getGameRoom(roomId).setStarted(true);
                    broadcast(roomId, "start", null, "game start");
                }
                break;

            case "unready":
                if (roomService.roomNotExist(roomId)) {
                    sendSimpleMsg(session, "room not exist");
                    break;
                } else if (roomService.roomStarted(roomId)) {
                    sendSimpleMsg(session, "game already started");
                    break;
                }

                roomService.markUnready(roomId, userId);
                broadcast(roomId, "unready", userId, "user unready");
                break;


            // guess operator
            case "guess":
                if (roomService.roomNotExist(roomId)) {
                    sendSimpleMsg(session, "room not exist");
                    break;
                } else if (!roomService.roomStarted(roomId)) {
                    sendSimpleMsg(session, "room not started");
                    break;
                } else if (!roomSessions.get(roomId).containsKey(userId) || !userRoomMap.containsKey(userId)) {
                    sendSimpleMsg(session, "user not in room");
                    break;
                }

                String guessName = msg.getGuess();
                Map<String, Object> payload = roomService.processGuess(roomId, userId, guessName);
                // to self
                sendPayload(roomId, userId, "guess", payload);
                // to opponent
                String opponentId = roomService.getGameRoom(roomId).getOpponentId(userId);
                payload.remove("guess");
                sendPayload(roomId, opponentId, "guess", payload);

                // when guess correct
                if (payload.get("correct").equals("true")) {
                    broadcast(roomId, "over", null, "game over");
                    // send to self opponent guesses
                    sendPayload(roomId, userId, "opponent guesses",
                            roomService.getPastGuesses(roomId, opponentId));
                    // send to opponent self guesses
                    sendPayload(roomId, opponentId, "opponent guesses",
                            roomService.getPastGuesses(roomId, userId));
                    roomService.roomGameOver(roomId);
                }
                break;

            case "suggest":
                break;

            default:
                break;
        }
    }

    private void broadcast(String roomId, String type, String sourceUserId, Object payload) throws IOException {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession s = entry.getValue();

            if (s.isOpen()) {
                WebSocketMessage outMsg = new WebSocketMessage(type, roomId, sourceUserId, payload);
                s.sendMessage(new TextMessage(objectMapper.writeValueAsString(outMsg)));
            }
        }
    }

    private void sendPayload(String roomId, String userId, String type, Object payload) throws IOException {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        assert sessions != null;
        WebSocketSession session = sessions.get(userId);
        assert session != null;

        if (session.isOpen()) {
            WebSocketMessage msg = new WebSocketMessage(type, roomId, userId, payload);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    private void sendSimpleMsg(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            WebSocketMessage msg = new WebSocketMessage("message", null, null, message);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws IOException {
        log.info("WebSocket closed: " + session.getId());

        String disconnectedUserId = null;
        String affectedRoomId = null;

        // iterate over every room to find the disconnected user
        for (Map.Entry<String, Map<String, WebSocketSession>> roomEntry : roomSessions.entrySet()) {
            String roomId = roomEntry.getKey();
            Map<String, WebSocketSession> userMap = roomEntry.getValue();

            Iterator<Map.Entry<String, WebSocketSession>> iterator = userMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, WebSocketSession> userEntry = iterator.next();
                if (userEntry.getValue().getId().equals(session.getId())) {
                    disconnectedUserId = userEntry.getKey();
                    affectedRoomId = roomId;

                    iterator.remove(); // remove session from room
                    break;
                }
            }
            if (disconnectedUserId != null) {
                break;
            }
        }

        if (disconnectedUserId != null && affectedRoomId != null) {
            userRoomMap.remove(disconnectedUserId);
            roomService.roomGameOver(affectedRoomId);

            // clear whole room entry if room empty
            if (roomSessions.get(affectedRoomId).isEmpty()) {
                roomSessions.remove(affectedRoomId);
            }
            roomService.checkRoomVacant(affectedRoomId);

            try {
                broadcast(affectedRoomId, "message", null,
                        "user " + disconnectedUserId + " disconnected");
            } catch (IOException e) {
                log.error("Error broadcasting disconnect message", e);
            }
        }
    }
}
