package com.Chanalyst.ChanalystV1.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerDTO {
    private Long id;
    private String text;
    private Long playerId;
    private String playerName;
    private int votes;
}
