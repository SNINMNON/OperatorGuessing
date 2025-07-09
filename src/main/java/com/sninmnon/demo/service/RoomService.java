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
            synchronized (room) {
                room.addPlayer(userId);
                log.info("GameRoom: {}, added player: {}", room, userId);
            }
        }
    }

    public void markReady(String roomId, String userId) {
        GameRoom room = roomMap.get(roomId);
        if (room != null) {
            room.setReady(userId, true);
            log.info("user {} set ready", userId);
        }
    }

    public void markUnready(String roomId, String userId) {
        GameRoom room = roomMap.get(roomId);
        if (room != null) {
            room.setReady(userId, false);
            log.info("user {} set unready", userId);
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

        if (guess == null) {
            log.warn("Guess name {} not found", guessName);
            return null;
        }

        GuessComparison comparison = new GuessComparison(guess, answer);
        room.recordGuess(userId, guess, comparison);

        Map<String, Object> guessResponse = new ConcurrentHashMap<>();
        guessResponse.put("comparison", comparison);
        guessResponse.put("guess", guess);
        guessResponse.put("correct", comparison.getName().equals("equal"));
        return guessResponse;
    }

    public List<Operator> getPastGuesses(String roomId, String userId) {
        GameRoom room = roomMap.get(roomId);
        List<String> names = room.getPastGuesses(userId);
        List<Operator> pastGuesses = Collections.synchronizedList(new ArrayList<>());
        /* no need for synchronization
        * since these lists are dedicated for one user, and each user uses one websocket
        */
        for (String name: names) {
            pastGuesses.add(opMapper.findByName(name));
        }
        return pastGuesses;
    }

    public void checkRoomVacant(String roomId) {
        GameRoom room = roomMap.get(roomId);
        if (room.getPlayerNumber() == 0) {
            roomMap.remove(roomId);
            log.info("Deleted room {}", roomId);
        }
    }

    public boolean roomNotExist(String roomId) {
        return roomId == null || roomMap.get(roomId) == null;
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
        log.info("room {} game over", roomId);
    }

    public void removePlayer(String roomId, String userId){
        GameRoom room = roomMap.get(roomId);
        if (room == null || userId == null) {
            return;
        }
        if (room.getPlayerIds().contains(userId)) {
            room.getPlayerIds().remove(userId);
            room.getFeedbackMap().remove(userId);
            room.getGuessNamesMap().remove(userId);
            room.getReadyMap().remove(userId);
        }
    }
}
