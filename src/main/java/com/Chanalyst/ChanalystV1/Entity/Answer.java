package com.Chanalyst.ChanalystV1.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Answer {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne private Player player;   // who wrote it
    @ManyToOne private Room room;
    @ManyToOne private Question question;

    private int round;       // round #
    private int sequence;    // question sequence #
    private String text;

    private int votes = 0;   // count of votes received
}