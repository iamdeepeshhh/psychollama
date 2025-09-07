package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.Entity.Answer;
import com.Chanalyst.ChanalystV1.DTO.AnswerDTO;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.AnswerRepository;
import com.Chanalyst.ChanalystV1.Repository.PlayerRepository;
import com.Chanalyst.ChanalystV1.Service.AnswerService;
import com.Chanalyst.ChanalystV1.Service.PlayerService;
import com.Chanalyst.ChanalystV1.Service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;
    private final RoomService roomService;
    private final PlayerService playerService;
    private final AnswerRepository answerRepository;
    private final PlayerRepository playerRepository;

    // Submit answer
    @PostMapping("/submit")
    public ResponseEntity<Answer> submitAnswer(
            @RequestParam Long playerId,
            @RequestParam String roomCode,
            @RequestParam int round,
            @RequestParam int sequence,
            @RequestParam String text
    ) {
        return ResponseEntity.ok(answerService.submitAnswer(playerId, roomCode, round, sequence, text));
    }

    @GetMapping("/list")
    public ResponseEntity<List<AnswerDTO>> getAnswersForQuestion(
            @RequestParam String roomCode,
            @RequestParam int round,
            @RequestParam int sequence
    ) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // Prefer repository method that eagerly loads player (via @EntityGraph) and orders for stability
        List<Answer> answers = answerRepository.findByRoomAndRoundAndSequenceOrderByIdAsc(room, round, sequence);

        List<AnswerDTO> dto = answers.stream()
                .map(a -> new AnswerDTO(
                        a.getId(),
                        a.getText(),
                        a.getPlayer() != null ? a.getPlayer().getId() : null,
                        a.getPlayer() != null ? a.getPlayer().getName() : null,
                        a.getVotes()
                ))
                .toList();
        return ResponseEntity.ok(dto);
    }

    // ==================================
    // Gate to move from "answer" to "vote"
    // true only when everyone expected has answered
    // ==================================
    @GetMapping("/all-answered")
    public ResponseEntity<Boolean> allAnswered(
            @RequestParam String roomCode,
            @RequestParam int round,
            @RequestParam int sequence
    ) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        long actual = answerRepository.countDistinctPlayersAnswered(room, round, sequence);

        // If you track `ready` on Player, this will be the expected number.
        // If not, fallback to all players in the room.
        long expected = playerRepository.countReadyPlayers(room);
        if (expected == 0) {
            expected = playerRepository.countPlayersInRoom(room);
        }

        boolean done = expected > 0 && actual >= expected;
        return ResponseEntity.ok(done);
    }

    @PostMapping("/vote/{answerId}")
    public ResponseEntity<Answer> vote(@PathVariable Long answerId) {
        Answer answer = answerRepository.findById(answerId).orElseThrow();
        answer.setVotes(answer.getVotes() + 1);
        return ResponseEntity.ok(answerRepository.save(answer));
    }



}
