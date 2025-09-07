package com.Chanalyst.ChanalystV1.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // keep this if you want to store just the code as well
    private String roomCode;

    private int points = 0;

    private boolean ready = false;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    // Custom constructor for quick creation
    public Player(String name, String roomCode) {
        this.name = name;
        this.roomCode = roomCode;
        this.points = 0;
        this.ready = false;
    }
}