package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.DTO.VoteResultsDto;
import com.Chanalyst.ChanalystV1.Entity.Answer;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Entity.Vote;
import com.Chanalyst.ChanalystV1.Repository.AnswerRepository;
import com.Chanalyst.ChanalystV1.Repository.PlayerRepository;
import com.Chanalyst.ChanalystV1.Repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.Chanalyst.ChanalystV1.DTO.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepo;
    private final AnswerRepository answerRepo;
    private final PlayerRepository playerRepo;

    private final RoomService roomService;

    public void registerVote(Long voterId, Long answerId) {
        Player voter = playerRepo.findById(voterId)
                .orElseThrow(() -> new RuntimeException("Voter not found"));
        Answer answer = answerRepo.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

        // Prevent self-voting
        if (answer.getPlayer().getId().equals(voter.getId())) {
            throw new RuntimeException("You cannot vote for your own answer!");
        }

        // Prevent multiple votes on the same question by the same voter
        Room room = answer.getRoom();
        int round = answer.getRound();
        int sequence = answer.getSequence();
        if (voteRepo.existsByVoterAndAnswer_RoomAndAnswer_RoundAndAnswer_Sequence(voter, room, round, sequence)) {
            throw new RuntimeException("You have already voted on this question!");
        }

        // Save vote
        Vote vote = new Vote();
        vote.setVoter(voter);
        vote.setAnswer(answer);
        voteRepo.save(vote);

        // Increment votes for answer
        answer.setVotes(answer.getVotes() + 1);
        answerRepo.save(answer);

        // Award points to the player who wrote the answer
        Player answerOwner = answer.getPlayer();
        answerOwner.setPoints(answerOwner.getPoints() + 10); // e.g. +10 per vote
        playerRepo.save(answerOwner);
    }

    public boolean allVoted(String roomCode, int round, int sequence) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // Expected voters = distinct players who submitted answers for this question
        long expected = answerRepo.countDistinctPlayersAnswered(room, round, sequence);
        long actual   = voteRepo.countDistinctVoters(room, round, sequence);

        // Only consider complete when at least one answer exists and
        // every answerer has cast a vote.
        return expected > 0 && actual >= expected;
    }

    public VoteResultsDto.VoteSummary results(String roomCode, int round, int sequence) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        long expected = playerRepo.countReadyPlayers(room);
        if (expected == 0) expected = playerRepo.countPlayersInRoom(room);

        List<Vote> votes = voteRepo.findWithVoterAndAnswer(room, round, sequence);

        // Group votes by answer
        Map<Long, VoteResultsDto.AnswerBucket> buckets = new LinkedHashMap<>();
        for (Vote v : votes) {
            Answer a = v.getAnswer();
            Long answerId = a.getId();

            buckets.computeIfAbsent(answerId, id -> new VoteResultsDto.AnswerBucket(
                    a.getId(),
                    a.getText(),
                    a.getPlayer() != null ? a.getPlayer().getId()   : null,
                    a.getPlayer() != null ? a.getPlayer().getName() : null,
                    0,
                    new ArrayList<>()
            ));

            VoteResultsDto.AnswerBucket b = buckets.get(answerId);
            // increment vote count
            buckets.put(answerId, new VoteResultsDto.AnswerBucket(
                    b.answerId(), b.text(), b.playerId(), b.playerName(),
                    b.votes() + 1,
                    appendVoter(b.voterIds(), v.getVoter())
            ));
        }

        // Flat list of voters (optional but handy for debugging)
        List<VoteResultsDto.VoterLine> voterLines = votes.stream()
                .map(v -> new VoteResultsDto.VoterLine(
                        v.getVoter().getId(),
                        v.getVoter().getName(),
                        v.getAnswer().getId()
                ))
                .collect(Collectors.toList());

        return new VoteResultsDto.VoteSummary(
                expected,
                votes.size(),
                new ArrayList<>(buckets.values()),
                voterLines
        );
    }

    private List<VoteResultsDto.SimpleUser> appendVoter(List<VoteResultsDto.SimpleUser> existing, Player voter) {
        List<VoteResultsDto.SimpleUser> copy = new ArrayList<>(existing);
        copy.add(new VoteResultsDto.SimpleUser(voter.getId(), voter.getName()));
        return copy;
    }
}
