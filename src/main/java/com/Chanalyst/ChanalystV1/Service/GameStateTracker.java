package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.DTO.GameState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameStateTracker {
    private final Map<String, GameState> stateByRoom = new ConcurrentHashMap<>();

    public void save(String roomCode, GameState state) {
        stateByRoom.put(roomCode, state);
    }

    public GameState getState(String roomCode) {
        return stateByRoom.getOrDefault(roomCode,
                new GameState(0, 0, null, "lobby"));
    }
}
