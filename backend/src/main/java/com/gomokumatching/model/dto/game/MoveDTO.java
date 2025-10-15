package com.gomokumatching.model.dto.game;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gomokumatching.model.GameMove;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    /**
     * Convert GameMove entity to MoveDTO.
     *
     * @param move GameMove entity from database
     * @return MoveDTO for API response
     */
    public static MoveDTO fromEntity(GameMove move) {
        // convert OffsetDateTime to LocalDateTime if needed
        LocalDateTime localTimestamp = null;
        if (move.getMoveTimestamp() != null) {
            localTimestamp = move.getMoveTimestamp()
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        return MoveDTO.builder()
                .moveNumber(move.getMoveNumber())
                .playerId(move.getPlayer() != null ? move.getPlayer().getPlayerId() : null)
                .isAI("AI".equals(move.getPlayerType().name()))
                .row(move.getBoardX())
                .col(move.getBoardY())
                .stoneColor(move.getStoneColor().name())
                .timestamp(localTimestamp)
                .timeTakenMs(move.getTimeTakenMs())
                .build();
    }
}
