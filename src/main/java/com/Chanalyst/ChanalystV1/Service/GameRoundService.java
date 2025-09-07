package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.Entity.GameRound;
import com.Chanalyst.ChanalystV1.Repository.GameRoundRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameRoundService {
    private final GameRoundRepository roundRepo;

    public GameRoundService(GameRoundRepository roundRepo) {
        this.roundRepo = roundRepo;
    }

    // Start new round
    public GameRound startRound(String roomId, int roundNumber, String question) {
        GameRound round = new GameRound(null, roomId, roundNumber, question, null);
        return roundRepo.save(round);
    }

    // Set winner for round
    public void setRoundWinner(String roomId, int roundNumber, String winner) {
        roundRepo.findByRoomIdAndRoundNumber(roomId, roundNumber).ifPresent(round -> {
            round.setWinner(winner);
            roundRepo.save(round);
        });
    }

    // Get all rounds in a room
    public List<GameRound> getRounds(String roomId) {
        return roundRepo.findByRoomId(roomId);
    }
}
