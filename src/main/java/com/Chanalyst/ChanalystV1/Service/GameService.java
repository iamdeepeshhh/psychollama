package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.PlayerRepository;
import com.Chanalyst.ChanalystV1.Repository.RoomRepository;
import lombok.RequiredArgsConstructor;
//import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Player markReady(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        player.setReady(true);
        playerRepository.save(player);

        // Check if all players in the room are ready
        List<Player> players = playerRepository.findByRoom_Code(player.getRoom().getCode());
        boolean allReady = players.stream().allMatch(Player::isReady);

        if (allReady) {
            // Update room state
            Room room = player.getRoom();
            room.setGameStarted(true);
            roomRepository.save(room);

            // Publish event to Kafka
//            kafkaTemplate.send("game-events", "game-start", room.getCode());
        }

        return player;
    }
}