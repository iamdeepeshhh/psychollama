package com.Chanalyst.ChanalystV1.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    private int round;          // current round number
    private int sequence;       // sequence of question in this round
    private String questionText; // the actual question text
    private String phase;       // current phase: "question", "answer", "vote", "result", "scoreboard"
    public GameState(int round, int sequence, String questionText) {
        this.round = round;
        this.sequence = sequence;
        this.questionText = questionText;
        this.phase = "question"; // default phase if not set
    }
}
