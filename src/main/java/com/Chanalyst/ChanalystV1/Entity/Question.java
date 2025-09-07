package com.Chanalyst.ChanalystV1.Entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "questions")
@Data
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    private boolean used = false;  // 🟢 mark if already asked
    private int round;

    private int sequence;
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;
}