package com.Chanalyst.ChanalystV1.Repository;

import com.Chanalyst.ChanalystV1.Entity.Answer;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    @Query("""
    select count(distinct v.voter.id)
    from Vote v
    where v.answer.room = :room
      and v.answer.round = :round
      and v.answer.sequence = :sequence
  """)
    long countDistinctVoters(@Param("room") Room room,
                             @Param("round") int round,
                             @Param("sequence") int sequence);

    // Results: fetch votes with voter + answer (+ answer.player) in one shot
    @Query("""
    select v
    from Vote v
      join fetch v.voter
      join fetch v.answer a
      left join fetch a.player
    where a.room = :room
      and a.round = :round
      and a.sequence = :sequence
    order by v.id asc
  """)
    List<Vote> findWithVoterAndAnswer(@Param("room") Room room,
                                      @Param("round") int round,
                                      @Param("sequence") int sequence);

    // Prevent double-vote for the same question by same voter
    boolean existsByVoterAndAnswer_RoomAndAnswer_RoundAndAnswer_Sequence(
            Player voter, Room room, int round, int sequence
    );
}
