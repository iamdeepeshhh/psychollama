package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.DTO.ScoreDto;
import com.Chanalyst.ChanalystV1.DTO.ScoreboardDto;
import com.Chanalyst.ChanalystV1.Entity.Answer;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Entity.Scoreboard;
import com.Chanalyst.ChanalystV1.Repository.AnswerRepository;
import com.Chanalyst.ChanalystV1.Repository.RoomRepository;
import com.Chanalyst.ChanalystV1.Repository.ScoreBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreboardService {
    private final ScoreBoardRepository scoreboardRepository;
    private final RoomRepository roomRepository;

    // Add points for a player in this round
    public void addPoints(Player player, Room room, int round, int points) {
        // Find existing row
        Scoreboard existing = scoreboardRepository
                .findByPlayerAndRoomAndRound(player, room, round)
                .orElse(null);

        int previousCumulative = scoreboardRepository
                .findTopByPlayerAndRoomOrderByRoundDesc(player, room)
                .map(Scoreboard::getCumulativePoints)
                .orElse(0);

        if (existing == null) {
            // New row for this round
            existing = Scoreboard.builder()
                    .player(player)
                    .room(room)
                    .round(round)
                    .points(points)
                    .cumulativePoints(previousCumulative + points)
                    .build();
        } else {
            // Update existing row
            existing.setPoints(existing.getPoints() + points);
            existing.setCumulativePoints(previousCumulative + points);
        }

        scoreboardRepository.save(existing);
    }

    public List<ScoreboardDto> getRoundScores(String roomCode, int round) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return scoreboardRepository.findRoundScoresDto(room, round);
    }

    public List<ScoreboardDto> getCumulativeScores(String roomCode) {
        Room room = roomRepository.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return scoreboardRepository.findAllScoresDto(room);
    }

}