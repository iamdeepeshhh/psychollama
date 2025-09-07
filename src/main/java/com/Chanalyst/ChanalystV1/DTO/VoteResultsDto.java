package com.Chanalyst.ChanalystV1.DTO;

import lombok.Data;

import java.util.List;

@Data
public class VoteResultsDto {
    // High-level summary the frontend consumes
    public record VoteSummary(
            long expectedVoters,
            long totalVotes,
            List<AnswerBucket> byAnswer,
            List<VoterLine> voters
    ) {}

    // Per-answer bucket for the pie: who authored it, how many votes, and which voters
    public record AnswerBucket(
            Long answerId,
            String text,
            Long playerId,
            String playerName,
            long votes,
            List<SimpleUser> voterIds
    ) {}

    // Just an id+name pair
    public record SimpleUser(
            Long id,
            String name
    ) {}

    // Flat view: each voter and which answer they voted for
    public record VoterLine(
            Long voterId,
            String voterName,
            Long votedAnswerId
    ) {}
}
