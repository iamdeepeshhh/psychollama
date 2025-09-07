package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.DTO.VoteResultsDto;
import com.Chanalyst.ChanalystV1.Service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<String> vote(
            @RequestParam Long voterId,
            @RequestParam Long answerId,
            @RequestParam String roomCode
    ) {
        voteService.registerVote(voterId, answerId, roomCode);
        return ResponseEntity.ok("Vote registered successfully!");
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