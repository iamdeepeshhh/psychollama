package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoomService {
    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room createRoom(String mode, String language) {
        Room room = new Room();
        room.setCode(UUID.randomUUID().toString().substring(0,6)); // generate 6-char code
        room.setGameStarted(false);
        room.setMode(mode != null ? mode.toLowerCase() : "normal"); // 👈 default = normal
        room.setLanguage(language != null ? language : "english");
        return roomRepository.save(room);
    }

    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> findByCode(String code) {
        return roomRepository.findByCode(code);
    }
}
