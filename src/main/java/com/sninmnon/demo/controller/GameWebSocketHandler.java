package com.sninmnon.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sninmnon.demo.service.OpService;
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
    @Autowired
    private OpService opService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: " + session.getId());
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws IOException {
        WebSocketMessage msg = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
        String userId = msg.getUserId();
        String roomId;

        log.info("Message received: type={}, user={}", msg.getType(), userId);

        switch (msg.getType()) {
            // create room and join
            case "create":
                if (userRoomMap.get(userId) != null) {
                    sendErrorMsg(session, "user already in room " + userRoomMap.get(userId));
                    break;
                }
                String newRoomId = roomService.createRoom(userId, msg.getDataBool("public"));

                roomSessions.put(newRoomId, new ConcurrentHashMap<>());
                roomSessions.get(newRoomId).put(userId, session);
                roomService.joinRoom(newRoomId, userId);
                userRoomMap.put(userId, newRoomId);
                sendPayload(newRoomId, userId, "roomId", newRoomId);
                break;

            // join existing room with roomId
            case "join":
                roomId = msg.getDataStr("roomId").trim().toUpperCase();
                if (userRoomMap.get(userId) != null) {
                    sendErrorMsg(session, "user already in room " + userRoomMap.get(userId));
                    break;
                } else if (roomService.roomNotExist(roomId)) {
                    sendErrorMsg(session, "room not exist");
                    break;
                } else if (roomService.roomStarted(roomId)) {
                    sendErrorMsg(session, "room already started");
                    break;
                }

                roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
                roomService.joinRoom(roomId, userId);
                userRoomMap.put(userId, roomId);
                broadcast(roomId, userId, "join");
                break;

            // find public room
            case "find":
                String publicRoomId = null;
                for (Map.Entry<String, Map<String, WebSocketSession>> roomEntry : roomSessions.entrySet()) {
                    String existRoomId = roomEntry.getKey();
                    if (roomService.isPublicGame(existRoomId) && roomService.getUserNumber(existRoomId) == 1) {
                        publicRoomId = existRoomId;
                        break;
                    }
                }
                if (publicRoomId != null) { // join public game room if there is one vacant
                    roomSessions.computeIfAbsent(publicRoomId, k -> new ConcurrentHashMap<>()).put(userId, session);
                    roomService.joinRoom(publicRoomId, userId);
                    userRoomMap.put(userId, publicRoomId);
                    broadcast(publicRoomId, userId, "join");
                    break;
                }
                sendErrorMsg(session, "no available public room");
                break;

            // mark ready for user in game room
            case "ready":
                roomId = userRoomMap.get(userId);
                if (roomService.roomNotExist(roomId)) {
                    sendErrorMsg(session, "room not exist");
                    break;
                } else if (roomService.roomStarted(roomId)) {
                    sendErrorMsg(session, "room already started");
                    break;
                } else if (roomService.getUserNumber(roomId) == 1) {
                    sendErrorMsg(session, "opponent not yet joined");
                    break;
                }

                roomService.markReady(roomId, userId);
                broadcast(roomId, userId, "ready");

                if (roomService.startRoom(roomId)) {
                    broadcast(roomId, null, "start");
                }
                break;

            case "unready":
                roomId = userRoomMap.get(userId);
                if (roomService.roomNotExist(roomId)) {
                    sendErrorMsg(session, "room not exist");
                    break;
                } else if (roomService.roomStarted(roomId)) {
                    sendErrorMsg(session, "game already started");
                    break;
                }

                roomService.markUnready(roomId, userId);
                broadcast(roomId, userId, "unready");
                break;


            // guess operator
            case "guess":
                roomId = userRoomMap.get(userId);
                if (roomService.roomNotExist(roomId)) {
                    sendErrorMsg(session, "room not exist");
                    break;
                } else if (!roomService.roomStarted(roomId)) {
                    sendErrorMsg(session, "room not started");
                    break;
                } else if (!roomSessions.get(roomId).containsKey(userId) || !userRoomMap.containsKey(userId)) {
                    sendErrorMsg(session, "user not in room");
                    break;
                }

                String guessName = msg.getDataStr("guess");
                Map<String, Object> guessResponse = roomService.processGuess(roomId, userId, guessName);
                // to self
                sendPayload(roomId, userId, "self guess", guessResponse);
                // to opponent
                String opponentId = roomService.getGameRoom(roomId).getOpponentId(userId);
                guessResponse.remove("guess");
                sendPayload(roomId, opponentId, "opponent guess", guessResponse);

                // when guess correct
                if (Boolean.TRUE.equals(guessResponse.get("correct"))) {
                    broadcastWin(roomId, userId, roomService.getGameRoom(roomId).getAnswer());
                    // send to self opponent history guesses
                    sendPayload(roomId, userId, "opponent history",
                            roomService.getPastGuesses(roomId, opponentId));
                    // send to opponent self history guesses
                    sendPayload(roomId, opponentId, "opponent history",
                            roomService.getPastGuesses(roomId, userId));
                    roomService.roomGameOver(roomId);
                }
                break;

            case "suggest":
                roomId = userRoomMap.get(userId);
                String query = msg.getDataStr("query");
                sendPayload(roomId, userId, "suggest", opService.suggestNames(query));
                break;

            default:
                sendErrorMsg(session, "message type unknown");
                break;
        }
    }

    private void broadcast(String roomId, String sourceUserId, String message) throws IOException {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        WebSocketMessage msg = new WebSocketMessage("broadcast");
        msg.setUserId(sourceUserId);
        msg.putData("roomId", roomId);
        msg.putData("message", message);
        TextMessage textMsg = new TextMessage(objectMapper.writeValueAsString(msg));
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession s = entry.getValue();
            if (s.isOpen()) {
                s.sendMessage(textMsg);
            }
        }
    }

    private void broadcastWin(String roomId, String sourceUserId, Object answer) throws IOException {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        WebSocketMessage msg = new WebSocketMessage("broadcast");
        msg.setUserId(sourceUserId);
        msg.putData("message", "win");
        msg.putData("answer", answer);
        TextMessage textMsg = new TextMessage(objectMapper.writeValueAsString(msg));
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession s = entry.getValue();
            if (s.isOpen()) {
                s.sendMessage(textMsg);
            }
        }
    }

    private void sendPayload(String roomId, String userId, String type, Object payload) throws IOException {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        assert sessions != null;
        WebSocketSession session = sessions.get(userId);
        assert session != null;

        if (session.isOpen()) {
            WebSocketMessage msg = new WebSocketMessage(type);
            msg.setUserId(userId);
            msg.putData("payload", payload);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    private void sendErrorMsg(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            WebSocketMessage msg = new WebSocketMessage("error");
            msg.putData("message", message);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws IOException {
        log.info("WebSocket closed: {}", session.getId());

        String disconnectedUserId = null;
        String affectedRoomId = null;

        // 查找并移除用户会话
        outer:
        for (Map.Entry<String, Map<String, WebSocketSession>> roomEntry : roomSessions.entrySet()) {
            String roomId = roomEntry.getKey();
            Map<String, WebSocketSession> userMap = roomEntry.getValue();

            synchronized (userMap) {
                for (Map.Entry<String, WebSocketSession> userEntry : userMap.entrySet()) {
                    if (userEntry.getValue().getId().equals(session.getId())) {
                        disconnectedUserId = userEntry.getKey();
                        affectedRoomId = roomId;
                        userMap.remove(disconnectedUserId);
                        break outer;
                    }
                }
            }
        }

        if (disconnectedUserId != null && affectedRoomId != null) {
            userRoomMap.remove(disconnectedUserId);
            log.info("User {} removed from room {}", disconnectedUserId, affectedRoomId);

            // 通知房间其他人
            try {
                broadcast(affectedRoomId, disconnectedUserId, "disconnect");
            } catch (Exception e) {
                log.error("Failed to broadcast disconnection", e);
            }

            // end game and remove user within room service
            roomService.removePlayer(affectedRoomId, disconnectedUserId);
            roomService.roomGameOver(affectedRoomId);

            // 如果房间空了，清理房间
            Map<String, WebSocketSession> remainingSessions = roomSessions.get(affectedRoomId);
            if (remainingSessions != null && remainingSessions.isEmpty()) {
                roomSessions.remove(affectedRoomId);
            }

            roomService.deleteRoom(affectedRoomId);  // 这个方法内部会移除空房
        } else {
            log.warn("Disconnected session {} not found in roomSessions", session.getId());
        }
    }
}