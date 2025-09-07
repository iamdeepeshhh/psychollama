package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Service.PlayerService;
import com.Chanalyst.ChanalystV1.Service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/player")
public class PlayerController {

    private final PlayerService playerService;
    private final RoomService roomService;

    public PlayerController(PlayerService playerService, RoomService roomService) {
        this.playerService = playerService;
        this.roomService = roomService;
    }

    // ✅ Join a room
    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinRoom(
            @RequestParam String name,
            @RequestParam String roomCode) {

        Room room = roomService.findByCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found!"));

        Player player = playerService.addPlayer(name, room);

        Map<String, Object> response = new HashMap<>();
        response.put("room", room);
        response.put("player", player);

        return ResponseEntity.ok(response);
    }


    // ✅ Get all players in a room
    @GetMapping("/{roomCode}")
    public ResponseEntity<List<Player>> getPlayersInRoom(@PathVariable String roomCode) {
        return roomService.findByCode(roomCode)
                .map(room -> ResponseEntity.ok(playerService.getPlayers(room)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Mark player as ready
    @PostMapping("/{playerId}/ready")
    public ResponseEntity<Player> markReady(@PathVariable Long playerId) {
        return ResponseEntity.ok(playerService.markReady(playerId));
    }

    // ✅ Check if all players are ready
    @GetMapping("/{roomCode}/all-ready")
    public ResponseEntity<Boolean> checkAllReady(@PathVariable String roomCode) {
        return roomService.findByCode(roomCode)
                .map(room -> ResponseEntity.ok(playerService.areAllPlayersReady(room)))
                .orElse(ResponseEntity.notFound().build());
    }
}