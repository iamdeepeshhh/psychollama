package com.Chanalyst.ChanalystV1.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "vote", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"voter_id", "answer_id"})
})
public class Vote {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne private Player voter;   // who voted
    @ManyToOne private Answer answer;  // chosen answer
}
