package com.gomokumatching.model.dto.game;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a single move in a game.
 * Used for move history and game replay.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveDTO {

    /**
     * Sequential move number (1-indexed)
     */
    private int moveNumber;

    /**
     * Player ID who made the move (null if AI)
     */
    private UUID playerId;

    /**
     * Whether this move was made by AI
     */
    private boolean isAI;

    /**
     * Row position (0-14)
     */
    private int row;

    /**
     * Column position (0-14)
     */
    private int col;

    /**
     * Stone color (BLACK or WHITE)
     */
    private String stoneColor;

    /**
     * Timestamp when move was made
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Time taken to make the move (milliseconds)
     */
    private Integer timeTakenMs;
}
