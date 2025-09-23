package com.Chanalyst.ChanalystV1.Repository;

import com.Chanalyst.ChanalystV1.DTO.ScoreboardDto;
import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Entity.Scoreboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScoreBoardRepository extends JpaRepository<Scoreboard, Long> {
    Optional<Scoreboard> findByPlayerAndRoomAndRound(Player player, Room room, int round);

    Optional<Scoreboard> findTopByPlayerAndRoomOrderByRoundDesc(Player player, Room room);

    List<Scoreboard> findByRoomAndRound(Room room, int round);

    List<Scoreboard> findByRoom(Room room);

    // ✅ Direct DTO projection for round scores
    @Query("SELECT new com.Chanalyst.ChanalystV1.DTO.ScoreboardDto(" +
            "p.id, p.name, r.code, s.round, s.points, s.cumulativePoints) " +
            "FROM Scoreboard s " +
            "JOIN s.player p " +
            "JOIN s.room r " +
            "WHERE r = :room AND s.round = :round")
    List<ScoreboardDto> findRoundScoresDto(@Param("room") Room room, @Param("round") int round);

    // ✅ Direct DTO projection for all cumulative scores
    @Query("SELECT new com.Chanalyst.ChanalystV1.DTO.ScoreboardDto(" +
            "p.id, p.name, r.code, s.round, s.points, s.cumulativePoints) " +
            "FROM Scoreboard s " +
            "JOIN s.player p " +
            "JOIN s.room r " +
            "WHERE r = :room")
    List<ScoreboardDto> findAllScoresDto(@Param("room") Room room);
}
