package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.Entity.Answer;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Question;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.AnswerRepository;
import com.Chanalyst.ChanalystV1.Repository.PlayerRepository;
import com.Chanalyst.ChanalystV1.Repository.QuestionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final RoomService roomService;
    private final PlayerService playerService;
    private final QuestionRepository questionRepository;
    private final PlayerRepository playerRepository;


    // Submit answer (idempotent)
    @Transactional
    public Answer submitAnswer(Long playerId, String roomCode, int round, int sequence, String text) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        Player player = playerService.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        Question question = questionRepository.findByRoomAndRoundAndSequence(room, round, sequence)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Prevent duplicate answers
        List<Answer> existing = answerRepository.findByRoomAndRoundAndSequenceAndPlayer(room, round, sequence, player);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        Answer answer = new Answer();
        answer.setPlayer(player);
        answer.setRoom(room);
        answer.setRound(round);
        answer.setSequence(sequence);
        answer.setQuestion(question);
        answer.setText(text);

        Answer saved = answerRepository.save(answer);

        // ✅ Check if all players answered
        long totalPlayers = playerRepository.countReadyPlayers(room);
        long answeredPlayers = answerRepository.countDistinctPlayersAnswered(room, round, sequence);

        if (totalPlayers == answeredPlayers) {
            question.setUsed(true);
            questionRepository.save(question);
        }

        return saved;
    }


    // Fetch all answers for a question
    public List<Answer> getAnswersForQuestion(String roomCode, int round, int sequence) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return answerRepository.findByRoomAndRoundAndSequenceOrderByIdAsc(room, round, sequence);
    }
}
