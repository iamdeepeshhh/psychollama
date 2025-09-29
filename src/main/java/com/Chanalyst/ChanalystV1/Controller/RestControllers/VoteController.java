package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.DTO.VoteResultsDto;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.PlayerRepository;
import com.Chanalyst.ChanalystV1.Repository.VoteRepository;
import com.Chanalyst.ChanalystV1.Service.RoomService;
import com.Chanalyst.ChanalystV1.Service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final RoomService roomService;
    private final PlayerRepository playerRepository;
    private final VoteRepository voteRepository;

    @PostMapping
    public ResponseEntity<String> vote(
            @RequestParam Long voterId,
            @RequestParam Long answerId,
            @RequestParam String roomCode
    ) {
        voteService.registerVote(voterId, answerId, roomCode);
        return ResponseEntity.ok("Vote registered successfully!");
    }

    // Check if player already voted for this round+sequence
    @GetMapping("/already-voted")
    public ResponseEntity<Boolean> hasPlayerVoted(
            @RequestParam Long playerId,
            @RequestParam String roomCode,
            @RequestParam int round,
            @RequestParam int sequence
    ) {
        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        boolean alreadyVoted = voteRepository.existsByVoterAndAnswer_RoomAndAnswer_RoundAndAnswer_Sequence(
                player, room, round, sequence
        );

        return ResponseEntity.ok(alreadyVoted);
    }


    @GetMapping("/all-voted")
    public ResponseEntity<Boolean> allVoted(
            @RequestParam String roomCode,
            @RequestParam int round,
            @RequestParam int sequence
    ) {
        return ResponseEntity.ok(voteService.allVoted(roomCode, round, sequence));
    }

    @GetMapping("/results")
    public ResponseEntity<VoteResultsDto.VoteSummary> results(
            @RequestParam String roomCode,
            @RequestParam int round,
            @RequestParam int sequence
    ) {
        return ResponseEntity.ok(voteService.results(roomCode, round, sequence));
    }

}