package com.sninmnon.demo.service;

import com.sninmnon.demo.entity.*;
import com.sninmnon.demo.mapper.OpMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RoomService {

    private final Map<String, GameRoom> roomMap = new ConcurrentHashMap<>(); // roomId -> GameRoom
    private final OpMapper opMapper;
    public RoomService(OpMapper opMapper) {this.opMapper = opMapper;}

    public String createRoom(String creatorId) {
        String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        //Operator answer = opMapper.getRandomOp();
        Operator answer = opMapper.findByName("阿");
        GameRoom room = new GameRoom(roomId, answer);
        room.addPlayer(creatorId);
        roomMap.put(roomId, room);
        log.info("GameRoom created: {}", room);
        return roomId;
    }

    public void joinRoom(String roomId, String userId) {
        GameRoom room = roomMap.get(roomId);
        if (room != null) {
            room.addPlayer(userId);
        }
    }

    public void markReady(String roomId, String userId) {
        GameRoom room = roomMap.get(roomId);
        if (room != null) {
            room.setReady(userId, true);
        }
    }

    public void markUnready(String roomId, String userId) {
        GameRoom room = roomMap.get(roomId);
        if (room != null) {
            room.setReady(userId, false);
        }
    }

    public GameRoom getGameRoom(String roomId) {
        return roomMap.get(roomId);
    }

    public Map<String, Object> processGuess(String roomId, String userId, String guessName) {
        GameRoom room = roomMap.get(roomId);
        if (room == null || !room.isStarted()) return null;

        Operator guess = opMapper.findByName(guessName);
        Operator answer = room.getAnswer();

        GuessFeedback feedback = new GuessFeedback(guess, answer);
        room.recordGuess(userId, guess, feedback);

        Map<String, Object> payload = new ConcurrentHashMap<>();
        payload.put("comparison", feedback);
        payload.put("guess", guess);
        if (feedback.getName().equals("equal")) {
            payload.put("correct", "true");
        } else {
            payload.put("correct", "false");
        }
        return payload;
    }

    public List<Operator> getPastGuesses(String roomId, String userId) {
        GameRoom room = roomMap.get(roomId);
        List<String> names = room.getPastGuesses(userId);
        List<Operator> pastGuesses = new ArrayList<>();
        for (String name: names) {
            pastGuesses.add(opMapper.findByName(name));
        }
        return pastGuesses;
    }

    public void checkRoomVacant(String roomId) {
        GameRoom room = roomMap.get(roomId);
        if (room != null && room.getPlayerNumber() == 0) {
            roomMap.remove(roomId);
            log.info("Deleted room {}", roomId);
        }
    }

    public boolean roomNotExist(String roomId) {
        if (roomId == null) {
            return false;
        }
        GameRoom room = roomMap.get(roomId);
        return room == null;
    }

    public boolean roomStarted(String roomId) {
        if (roomId == null) {
            return false;
        }
        GameRoom room = roomMap.get(roomId);
        return room.isStarted();
    }

    public void roomGameOver(String roomId) {
        GameRoom room = roomMap.get(roomId);
        room.setStarted(false);
        room.resetMaps();
    }
}
