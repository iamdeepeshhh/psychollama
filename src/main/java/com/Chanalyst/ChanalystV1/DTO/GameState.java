package com.Chanalyst.ChanalystV1.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    private int round;          // current round number
    private int sequence;
    private Set<String> acks = new HashSet<>();// sequence of question in this round
    private String questionText; // the actual question text
    private String phase;       // current phase: "question", "answer", "vote", "result", "scoreboard"
    // 👇 new fields
    private List<AnswerDTO> answers;   // only set when phase = "vote"
    private VoteResultsDto.VoteSummary results; // only set when phase = "result"
    private List<ScoreboardDto> scores;
    private boolean isLastSequence;
    private String message;
    // only set when phase = "scoreboard"
    private List<Long> votedPlayerIds;
    private int expectedVoters;
    private int actualVoters;
    private String waitingMessage;
    public GameState(int round, int sequence, String questionText) {
        this.round = round;
        this.sequence = sequence;
        this.questionText = questionText;
        this.phase = "question"; // default phase
    }
    public GameState(int round, int sequence, String questionText, String phase) {
        this.round = round;
        this.sequence = sequence;
        this.questionText = questionText;
        this.phase = phase;
    }

}
