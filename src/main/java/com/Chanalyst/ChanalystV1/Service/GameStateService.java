package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.DTO.*;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.AnswerRepository;
import com.Chanalyst.ChanalystV1.Repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameStateService {
    private final SimpMessagingTemplate messagingTemplate;
    private final AnswerRepository answerRepository;
    private final PlayerRepository playerRepository;
    private final GameStateTracker tracker;


    public void broadcastVotePhase(Room room, int round, int sequence) {
        List<AnswerDTO> dto = answerRepository
                .findByRoomAndRoundAndSequenceOrderByIdAsc(room, round, sequence)
                .stream()
                .map(a -> new AnswerDTO(
                        a.getId(),
                        a.getText(),
                        a.getPlayer() != null ? a.getPlayer().getId() : null,
                        a.getPlayer() != null ? a.getPlayer().getName() : null,
                        a.getVotes()
                ))
                .toList();

        String questionText = dto.isEmpty()
                ? "Question not found"
                : answerRepository.findByRoomAndRoundAndSequenceOrderByIdAsc(room, round, sequence)
                .get(0)
                .getQuestion().getText();

        GameState state = new GameState(round, sequence, questionText, "vote");
        state.setAnswers(dto);

        tracker.save(room.getCode(), state);
        messagingTemplate.convertAndSend("/topic/room/" + room.getCode(), state);
    }

    public void broadcastResultPhase(Room room, int round, int sequence, VoteResultsDto.VoteSummary results) {
        GameState state = new GameState(round, sequence, null, "result");
        state.setResults(results);
        messagingTemplate.convertAndSend("/topic/room/" + room.getCode(), state);
    }

    public void broadcastScoreboard(Room room, List<ScoreboardDto> scores) {
        GameState state = new GameState(0, 0, null, "scoreboard");
        state.setScores(scores);
        messagingTemplate.convertAndSend("/topic/room/" + room.getCode(), state);
    }

    public void broadcastWaitingRoom(Room room, int round, int sequence, String message) {
        GameState state = new GameState(round, sequence, null, "waitingRoom");
        state.setMessage(message);
        messagingTemplate.convertAndSend("/topic/room/" + room.getCode(), state);
    }


}
