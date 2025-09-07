package com.Chanalyst.ChanalystV1.Repository;

import com.Chanalyst.ChanalystV1.Entity.GameRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRoundRepository extends JpaRepository<GameRound, Long> {
    List<GameRound> findByRoomId(String roomId);
    Optional<GameRound> findByRoomIdAndRoundNumber(String roomId, int roundNumber);
}