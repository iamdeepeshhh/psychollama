package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.DTO.GameState;
import com.Chanalyst.ChanalystV1.Entity.Question;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Service.PlayerService;
import com.Chanalyst.ChanalystV1.Service.QuestionGeneratorService;
import com.Chanalyst.ChanalystV1.Service.ScoreboardService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class QuestionSyncController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerService playerService;
    private final ScoreboardService scoreboardService;
    private final QuestionGeneratorService questionGeneratorService;
    // Room states in memory for now (can later move to DB/Redis)
    private final Map<String, GameState> rooms = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> resultsAckMap = new ConcurrentHashMap<>();
    public QuestionSyncController(SimpMessagingTemplate messagingTemplate, PlayerService playerService, ScoreboardService scoreboardService, QuestionGeneratorService questionGeneratorService) {
        this.messagingTemplate = messagingTemplate;
        this.playerService = playerService;
        this.scoreboardService = scoreboardService;
        this.questionGeneratorService = questionGeneratorService;
    }

    // 🚀 Start round -> broadcast first question
    @MessageMapping("/room/{roomCode}/round/{round}/start")
    public void startRound(@DestinationVariable String roomCode,
                           @DestinationVariable int round) {
        List<Question> questions = questionGeneratorService.getQuestionsByRoomAndRound(roomCode, round);

        if (!questions.isEmpty()) {
            Question first = questions.get(0);

            GameState state = new GameState(round, first.getSequence(), first.getText(), "question");
            rooms.put(roomCode, state);

            messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);
        }
    }

    @MessageMapping("/room/{roomCode}/round/{round}/results-ack")
    public void ackResults(@DestinationVariable String roomCode,
                           @DestinationVariable int round,
                           @Payload Map<String, Object> payload) {

        Long playerId = ((Number) payload.get("playerId")).longValue();

        resultsAckMap
                .computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet())
                .add(playerId);

        int totalPlayers = playerService.getPlayersByRoomCode(roomCode).size();
        int acks = resultsAckMap.get(roomCode).size();

        if (acks >= totalPlayers) {
            // ✅ All players acknowledged → move to next question
            nextQuestion(roomCode, round);
            resultsAckMap.get(roomCode).clear(); // reset for next sequence
        }
    }

    // ⏭️ Move to the next question in the same round
//    @MessageMapping("/room/{roomCode}/round/{round}/next")
    public void nextQuestion(@DestinationVariable String roomCode,
                             @DestinationVariable int round) {
        GameState state = rooms.get(roomCode);
        if (state == null) return;

        List<Question> questions = questionGeneratorService.getQuestionsByRoomAndRound(roomCode, round);

        int nextSeq = state.getSequence() + 1;
        if (nextSeq <= questions.size()) {
            Question next = questions.get(nextSeq - 1);
            state.setSequence(nextSeq);
            state.setQuestionText(next.getText());
            state.setPhase("question");

            rooms.put(roomCode, state);

            messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);
        } else {
            // ✅ end of round -> go to scoreboard
            System.out.println("scoreboard please");
            state.setPhase("scoreboard");
            state.setScores(scoreboardService.getRoundScores(roomCode,round));
            messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);

            int nextRound = round + 1;
            int MAX_ROUNDS = 5;

            if (nextRound <= MAX_ROUNDS) {
                // 🚀 Automatically trigger next round after delay
                new Thread(() -> {
                    try {
                        Thread.sleep(10000); // 10s delay before new round starts
                        startRound(roomCode, nextRound);
                    } catch (InterruptedException ignored) {}
                }).start();
            } else {
                // ✅ All rounds finished → final scoreboard
                state.setPhase("final-scoreboard");
                state.setScores(scoreboardService.getCumulativeScores(roomCode));

                messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);
            }
        }
    }
}
