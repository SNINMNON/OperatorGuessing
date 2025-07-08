package com.sninmnon.demo.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class GameRoom {
    @Getter
    private final String roomId;
    private final List<String> playerIds = new ArrayList<>();
    private final Map<String, Boolean> readyMap = new HashMap<>(); // playerID -> ready
    private final Map<String, List<GuessFeedback>> feedbackMap = new HashMap<>(); // playerID -> past feedbacks
    private final Map<String, List<String>> guessNamesMap = new HashMap<>(); // playerID -> past guesses
    @Getter
    private final Operator answer;
    @Getter
    @Setter
    private boolean started = false;

    public GameRoom(String roomId, Operator answer) {
        this.roomId = roomId;
        this.answer = answer;
    }

    public void addPlayer(String userId) {
        if (playerIds.size() < 2 && !playerIds.contains(userId)) {
            playerIds.add(userId);
            readyMap.put(userId, false);
            feedbackMap.put(userId, new ArrayList<>());
            guessNamesMap.put(userId, new ArrayList<>());
        }
    }

    public boolean allReady() {
        return readyMap.size() == 2 && readyMap.values().stream().allMatch(Boolean::booleanValue);
    }

    public void setReady(String userId, boolean ready) {
        readyMap.put(userId, ready);
    }

    public List<GuessFeedback> getFeedbacks(String userId) {
        return feedbackMap.get(userId);
    }

    public List<String> getPastGuesses(String userId) {
        return guessNamesMap.get(userId);
    }

    public void recordGuess(String userId, Operator guess, GuessFeedback feedback) {
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
        this.feedbackMap.clear();
        this.guessNamesMap.clear();
        this.readyMap.clear();
    }
}
