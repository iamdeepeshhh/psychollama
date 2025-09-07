package com.Chanalyst.ChanalystV1.Repository;

import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByRoom(Room room);

    List<Player> findByRoom_Code(String roomCode);

    @Query("select count(p) from Player p where p.room = :room")
    long countPlayersInRoom(@Param("room") Room room);

    // PlayerRepository
    @Query("select count(p) from Player p where p.room = :room and p.ready = true")
    long countReadyPlayers(@Param("room") Room room);

    long countByRoom(Room room);

}