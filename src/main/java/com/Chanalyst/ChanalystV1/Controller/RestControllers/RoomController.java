package com.Chanalyst.ChanalystV1.Controller.RestControllers;


import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
//import com.Chanalyst.ChanalystV1.Service.GameProducer;
import com.Chanalyst.ChanalystV1.Service.PlayerService;
import com.Chanalyst.ChanalystV1.Service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/room")
public class RoomController {
    private final RoomService roomService;
    private final PlayerService playerService;

    public RoomController(RoomService roomService, PlayerService playerService) {
        this.roomService = roomService;
        this.playerService = playerService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRoom(
            @RequestParam String playerName,
            @RequestParam(defaultValue = "classic") String mode,
            @RequestParam(defaultValue = "english") String language) {

        Room room = roomService.createRoom(mode, language);
        Player host = playerService.addPlayer(playerName, room);

        Map<String, Object> response = new HashMap<>();
        response.put("room", room);
        response.put("player", host);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Room> getRoom(@PathVariable String code) {
        return roomService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
