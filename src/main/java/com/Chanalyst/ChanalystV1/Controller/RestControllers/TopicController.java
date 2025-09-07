package com.Chanalyst.ChanalystV1.Controller.RestControllers;

import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Entity.Topic;
import com.Chanalyst.ChanalystV1.Repository.TopicRepository;
import com.Chanalyst.ChanalystV1.Service.PlayerService;
import com.Chanalyst.ChanalystV1.Service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/topic")
public class TopicController {

    private final TopicRepository topicRepository;
    private final RoomService roomService;
    private final PlayerService playerService;

    public TopicController(TopicRepository topicRepository, RoomService roomService, PlayerService playerService) {
        this.topicRepository = topicRepository;
        this.roomService = roomService;
        this.playerService = playerService;
    }

    // ✅ Submit a topic
    @PostMapping("/submit")
    public ResponseEntity<Topic> submitTopic(
            @RequestParam Long playerId,
            @RequestParam String roomCode,
            @RequestParam String name
    ) {
        Room room = roomService.findByCode(roomCode).orElseThrow();
        Player player = playerService.findById(playerId).orElseThrow();

        Topic topic = new Topic();
        topic.setName(name);
        topic.setRoom(room);
        topic.setPlayer(player);

        return ResponseEntity.ok(topicRepository.save(topic));
    }

    // ✅ Get all topics in a room
    @GetMapping("/{roomCode}")
    public ResponseEntity<List<Topic>> getTopics(@PathVariable String roomCode) {
        return ResponseEntity.ok(topicRepository.findByRoom_Code(roomCode));
    }
}

