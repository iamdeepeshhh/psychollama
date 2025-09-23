package com.Chanalyst.ChanalystV1.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreboardDto {

    private Long playerId;
    private String playerName;
    private String roomCode;

    private int round;
    private int points;           // points earned in this round
    private int cumulativePoints; // running total

    public ScoreboardDto(Long playerId, String playerName, int round, int points, int cumulativePoints) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.round = round;
        this.points = points;
        this.cumulativePoints = cumulativePoints;
    }

}