package com.Chanalyst.ChanalystV1.Repository;

import com.Chanalyst.ChanalystV1.Entity.Answer;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @EntityGraph(attributePaths = "player")
    List<Answer> findByRoomAndRoundAndSequenceOrderByIdAsc(Room room, int round, int sequence);

    // AnswerRepository
    @Query("""
              select count(distinct a.player.id)
              from Answer a
              where a.room = :room and a.round = :round and a.sequence = :sequence
            """)
    long countDistinctPlayersAnswered(@Param("room") Room room,
                                      @Param("round") int round,
                                      @Param("sequence") int sequence);


    // PlayerRepository
    @Query("""
  select count(p)
  from Player p
  where p.room = :room and p.ready = true
""")
    long countReadyPlayers(@Param("room") Room room);

    // fetch all answers for a round
    List<Answer> findByRoomAndRound(Room room, int round);

    // fetch all answers by player in a round (if you want validations)
    List<Answer> findByRoomAndRoundAndPlayer(Room room, int round, Player player);

    List<Answer> findByRoom(Room room);

    // fetch answers for a specific question by a player (to prevent duplicates)
    List<Answer> findByRoomAndRoundAndSequenceAndPlayer(Room room, int round, int sequence, Player player);
}
