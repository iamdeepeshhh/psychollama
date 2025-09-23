package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Service.GameService;
import com.Chanalyst.ChanalystV1.Service.RoomService;
import com.Chanalyst.ChanalystV1.Service.ScoreboardService;
import com.Chanalyst.ChanalystV1.Service.ScoreboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final RoomService roomService;
    private final ScoreboardService scoreBoardService;



    @PostMapping("/ready/{playerId}")
    public ResponseEntity<Player> ready(@PathVariable Long playerId) {
        Player updatedPlayer = gameService.markReady(playerId);
        return ResponseEntity.ok(updatedPlayer);
    }

//    @GetMapping("/scoreboard/{roomCode}")
//    public ResponseEntity<List<ScoreDto>> getScoreboard(@PathVariable String roomCode) {
//        Room room = roomService.findByCode(roomCode)
//                .orElseThrow(() -> new RuntimeException("Room not found!"));
//
//        return ResponseEntity.ok(scoreService.getScoresForRoom(room));
//    }

}