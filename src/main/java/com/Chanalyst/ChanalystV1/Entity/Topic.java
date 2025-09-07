package com.Chanalyst.ChanalystV1.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;
}
