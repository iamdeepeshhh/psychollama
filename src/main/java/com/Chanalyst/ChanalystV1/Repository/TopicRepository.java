package com.Chanalyst.ChanalystV1.Repository;

import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByRoom(Room room);
    List<Topic> findByRoom_Code(String roomCode);
    long countByRoom(Room room);
}
