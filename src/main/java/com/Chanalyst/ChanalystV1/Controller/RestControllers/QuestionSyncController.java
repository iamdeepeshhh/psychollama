package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.DTO.GameState;
import com.Chanalyst.ChanalystV1.Entity.Question;
import com.Chanalyst.ChanalystV1.Service.QuestionGeneratorService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class QuestionSyncController {

    private final SimpMessagingTemplate messagingTemplate;
    private final QuestionGeneratorService questionGeneratorService;
    // Room states in memory for now (can later move to DB/Redis)
    private final Map<String, GameState> rooms = new ConcurrentHashMap<>();

    public QuestionSyncController(SimpMessagingTemplate messagingTemplate, QuestionGeneratorService questionGeneratorService) {
        this.messagingTemplate = messagingTemplate;
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

    // ⏭️ Move to the next question in the same round
    @MessageMapping("/room/{roomCode}/round/{round}/next")
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
            state.setPhase("scoreboard");
            messagingTemplate.convertAndSend("/topic/room/" + roomCode, state);
        }
    }
}
