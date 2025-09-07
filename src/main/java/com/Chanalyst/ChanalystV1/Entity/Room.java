package com.Chanalyst.ChanalystV1.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // Room code players enter

    private int totalRounds = 5;

    private String mode = "normal";  // default mode

    private String language;
    private boolean gameStarted = false;
}
