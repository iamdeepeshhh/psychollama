package com.Chanalyst.ChanalystV1.Service;

import com.Chanalyst.ChanalystV1.Entity.Player;
import com.Chanalyst.ChanalystV1.Entity.Room;
import com.Chanalyst.ChanalystV1.Entity.Topic;
import com.Chanalyst.ChanalystV1.Repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final TopicRepository topicRepository;

    public Topic addTopic(String name, Room room, Player player) {
        Topic topic = new Topic();
        topic.setName(name);
        topic.setRoom(room);
        topic.setPlayer(player);
        return topicRepository.save(topic);
    }

    public List<Topic> getTopicsByRoom(Room room) {
        return topicRepository.findByRoom(room);
    }
}
