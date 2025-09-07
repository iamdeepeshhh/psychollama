package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.DTO.ScoreDto;
import com.Chanalyst.ChanalystV1.Entity.Answer;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final AnswerRepository answerRepository;

    public List<ScoreDto> getScoresForRoom(Room room) {
        List<Answer> answers = answerRepository.findByRoom(room);

        Map<Player, Integer> points = new HashMap<>();
        for (Answer a : answers) {
            points.merge(a.getPlayer(), a.getVotes(), Integer::sum);
        }

        return points.entrySet().stream()
                .map(e -> new ScoreDto(e.getKey().getId(), e.getKey().getName(), e.getValue()))
                .sorted(Comparator.comparingInt(ScoreDto::points).reversed())
                .toList();
    }
}