package com.Chanalyst.ChanalystV1.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_rounds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId;
    private int roundNumber;
    private String question;
    private String winner;
}