package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.DTO.QuestionDto;
import com.Chanalyst.ChanalystV1.Entity.Question;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.QuestionRepository;
import com.Chanalyst.ChanalystV1.Service.QuestionGeneratorService;
import com.Chanalyst.ChanalystV1.Service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/game")
public class QuestionController {

    private final QuestionGeneratorService questionService;
    private final RoomService roomService;
    private final QuestionRepository questionRepository;
    public QuestionController(QuestionGeneratorService questionService, RoomService roomService, QuestionRepository questionRepository) {
        this.questionService = questionService;
        this.roomService = roomService;
        this.questionRepository = questionRepository;
    }

    // Generate 25 upfront
//    @PostMapping("/generate/{roomCode}")
//    public ResponseEntity<String> generate(@PathVariable String roomCode) {
//        questionService.generateFunnyQuestions(roomCode);
//        return ResponseEntity.ok("Questions generated!");
//    }

    @GetMapping("/questions/{roomCode}/round/{round}")
    public ResponseEntity<List<QuestionDto>> getQuestionsForRound(
            @PathVariable String roomCode,
            @PathVariable int round
    ) {
        List<QuestionDto> dtos = questionService.getQuestionsByRoomAndRound(roomCode, round)
                .stream()
                .map(q -> new QuestionDto(q.getId(), q.getText(), q.getRound(), q.getSequence(), q.isUsed()))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/starter-questions/{roomCode}")
    public ResponseEntity<List<Question>> getStarterQuestions(@PathVariable String roomCode) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Fetch only round 1 starter questions
        List<Question> starters = questionRepository.findByRoomAndRoundOrderBySequenceAsc(room, 1);

        return ResponseEntity.ok(starters);
    }

    // Fetch next one per round
//    @GetMapping("/next/{roomCode}")
//    public ResponseEntity<Question> next(@PathVariable String roomCode) {
//        return ResponseEntity.ok(questionService.getNextQuestion(roomCode));
//    }

    // Cleanup after game
    @DeleteMapping("/clear/{roomCode}")
    public ResponseEntity<String> clear(@PathVariable String roomCode) {
        questionService.clearQuestions(roomCode);
        return ResponseEntity.ok("Questions cleared!");
    }
}
