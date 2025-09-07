package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final QuestionGeneratorService questionGeneratorService;

    public PlayerService(PlayerRepository playerRepository,
                         QuestionGeneratorService questionGeneratorService) {
        this.playerRepository = playerRepository;
        this.questionGeneratorService = questionGeneratorService;
    }

    public Player addPlayer(String name, Room room) {
        Player player = new Player();
        player.setName(name);
        player.setPoints(0);
        player.setReady(false);
        player.setRoom(room);
        player.setRoomCode(room.getCode());
        return playerRepository.save(player);
    }

    public List<Player> getPlayers(Room room) {
        return playerRepository.findByRoom(room);
    }

    public Player markReady(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        player.setReady(true);

        // ✅ Save immediately so DB reflects this player's ready state
        player = playerRepository.save(player);

        Room room = player.getRoom();
        if (areAllPlayersReady(room)) {
            // ✅ Run generation async so it doesn't block the Ready response
            CompletableFuture.runAsync(() -> {
                questionGeneratorService.generateQuestions(room.getCode(), 5, room.getMode(), 1, room.getLanguage());
                questionGeneratorService.generateQuestions(room.getCode(), 5, room.getMode(), 2, room.getLanguage());
                questionGeneratorService.generateQuestions(room.getCode(), 5, room.getMode(), 3, room.getLanguage());
                questionGeneratorService.generateQuestions(room.getCode(), 5, room.getMode(), 4, room.getLanguage());
                questionGeneratorService.generateQuestions(room.getCode(), 5, room.getMode(), 5, room.getLanguage());
            });
        }

        return player;
    }


    public boolean areAllPlayersReady(Room room) {
        List<Player> players = playerRepository.findByRoom(room);
        return players.stream().allMatch(Player::isReady);
    }

    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }
}
