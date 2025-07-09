package com.sninmnon.demo.entity;

import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GameRoom {
    private final String roomId;

    private final List<String> playerIds = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Boolean> readyMap = new ConcurrentHashMap<>(); // playerID -> ready
    private final Map<String, List<GuessComparison>> feedbackMap = new ConcurrentHashMap<>(); // playerID -> past feedbacks
    private final Map<String, List<String>> guessNamesMap = new ConcurrentHashMap<>(); // playerID -> past guesses

    private final Operator answer;
    private boolean started = false;

    public GameRoom(String roomId, Operator answer) {
        this.roomId = roomId;
        this.answer = answer;
    }

    @Override
    public String toString() {
        return "GameRoom{" +
                "roomId='" + roomId + '\'' +
                ", playerIds=" + playerIds +
                ", started=" + started +
                '}';
    }

    // this is not concurrent safe, should be used only for local debugging purpose
    public void printInfo() {
        StringBuilder msg = new StringBuilder();
        msg.append("roomId=").append(roomId);
        for (String userId: this.playerIds) { // should be wrapped inside synchronized
            msg.append("\tuserId=").append(userId);
            msg.append("\tfeedbacks=").append(this.feedbackMap.get(userId));
            msg.append("\tguessNames=").append(this.guessNamesMap.get(userId));
        }
        msg.append("\treadyMap=").append(this.readyMap);
        System.out.println(msg);
    }

    public void addPlayer(String userId) {
        if (playerIds.size() < 2 && !playerIds.contains(userId)) {
            playerIds.add(userId);
            readyMap.put(userId, false);
            feedbackMap.put(userId, Collections.synchronizedList(new ArrayList<>()));
            guessNamesMap.put(userId, Collections.synchronizedList(new ArrayList<>()));
        }
    }

    public boolean allReady() {
        return playerIds.size() == 2 &&
                readyMap.size() == 2 &&
                readyMap.values().stream().allMatch(Boolean::booleanValue);
    }


    public void setReady(String userId, boolean ready) {
        readyMap.put(userId, ready);
    }

    public List<String> getPastGuesses(String userId) {
        return guessNamesMap.get(userId);
    }

    public void recordGuess(String userId, Operator guess, GuessComparison feedback) {
        feedbackMap.get(userId).add(feedback);
        guessNamesMap.get(userId).add(guess.getName());
    }

    public String getOpponentId(String userId) {
        return playerIds.stream().filter(id -> !id.equals(userId)).findFirst().orElse(null);
    }

    public Integer getPlayerNumber() {
        return playerIds.size();
    }

    public void resetMaps() {
        synchronized (this.playerIds) {
            for (String userId: this.playerIds) {
                this.feedbackMap.get(userId).clear();
                this.guessNamesMap.get(userId).clear();
                this.readyMap.replace(userId, false);
            }
            // clear players who dropped out of room
            this.readyMap.keySet().removeIf(userId -> !this.playerIds.contains(userId));
        }
    }
}
