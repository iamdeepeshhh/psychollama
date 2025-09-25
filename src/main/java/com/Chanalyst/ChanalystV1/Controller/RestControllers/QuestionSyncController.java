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
    private final Map<String, Set<String>> triggeredNext = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> triggeredRounds = new ConcurrentHashMap<>();

    public QuestionSyncController(SimpMessagingTemplate messagingTemplate,
                                  PlayerService playerService,
                                  ScoreboardService scoreboardService,
                                  QuestionGeneratorService questionGeneratorService) {
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
        System.out.println("▶️ Starting Round " + round + " for room " + roomCode +
                " | Questions fetched = " + questions.size());

        // 🚦 Deduplication: only start once per (room, round)
        Set<Integer> started = triggeredRounds.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet());
        if (!started.add(round)) {
            return;
        }

        if (!questions.isEmpty()) {
            Question first = questions.get(0);

            GameState state = new GameState(round, first.getSequence(), first.getText(), "question");
            rooms.put(roomCode, state);

            System.out.println("✅ First Question: seq=" + first.getSequence() + " text=" + first.getText());
            messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);
        } else {
            System.out.println("⚠️ No questions found for round " + round + " in room " + roomCode);
        }

        // Reset acks for the new round
        resultsAckMap.put(roomCode, ConcurrentHashMap.newKeySet());
    }

    @MessageMapping("/room/{roomCode}/round/{round}/results-ack")
    public void ackResults(@DestinationVariable String roomCode,
                           @DestinationVariable int round,
                           @Payload Map<String, Object> payload) {

        Long playerId = ((Number) payload.get("playerId")).longValue();
        resultsAckMap.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet()).add(playerId);

        int totalPlayers = playerService.getPlayersByRoomCode(roomCode).size();
        int acks = resultsAckMap.get(roomCode).size();

        GameState state = rooms.get(roomCode);

        System.out.println("📩 Ack from player=" + playerId +
                " | acks=" + acks + "/" + totalPlayers +
                " | room=" + roomCode + " round=" + round +
                " seq=" + (state != null ? state.getSequence() : "?"));

        if (acks >= totalPlayers && state != null) {
            String key = roomCode + "-" + round + "-" + state.getSequence();

            Set<String> triggered = triggeredNext.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet());
            if (triggered.add(key)) {
                System.out.println("✅ All acks received → moving to next question (key=" + key + ")");
                nextQuestion(roomCode, round);
                resultsAckMap.get(roomCode).clear();
            } else {
                System.out.println("⚠️ Duplicate ack ignored for key=" + key);
            }
        }
    }

    // ⏭️ Move to the next question in the same round
    public void nextQuestion(@DestinationVariable String roomCode,
                             @DestinationVariable int round) {
        GameState state = rooms.get(roomCode);
        if (state == null) {
            System.out.println("⚠️ No active game state found for room " + roomCode);
            return;
        }

        List<Question> questions = questionGeneratorService.getQuestionsByRoomAndRound(roomCode, round);
        int nextSeq = state.getSequence() + 1;

        System.out.println("➡️ Next Question check: currentSeq=" + state.getSequence() +
                " nextSeq=" + nextSeq + " | totalQuestions=" + questions.size());

        if (nextSeq <= questions.size()) {
            Question next = questions.get(nextSeq - 1);
            state.setSequence(nextSeq);
            state.setQuestionText(next.getText());
            state.setPhase("question");

            rooms.put(roomCode, state);

            System.out.println("📝 Sending Question seq=" + nextSeq + " | text=" + next.getText());
            messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);
        } else {
            System.out.println("📊 End of round " + round + " → showing scoreboard");
            state.setPhase("scoreboard");
            state.setScores(scoreboardService.getRoundScores(roomCode, round));
            messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);

            int nextRound = round + 1;
            int MAX_ROUNDS = 5;

            if (nextRound <= MAX_ROUNDS) {
                System.out.println("⏳ Waiting 10s then starting Round " + nextRound);
                new Thread(() -> {
                    try {
                        Thread.sleep(10000);
                        startRound(roomCode, nextRound);
                    } catch (InterruptedException e) {
                        System.out.println("❌ Round start interrupted: " + e.getMessage());
                    }
                }).start();
            } else {
                System.out.println("🏆 All rounds completed → Final Scoreboard");
                state.setPhase("final-scoreboard");
                state.setScores(scoreboardService.getCumulativeScores(roomCode));

                rooms.put(roomCode, state);
                messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);
            }
        }
    }
}