package com.gomokumatching.model.dto.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Kafka event for individual game moves.
 *
 * Published to topic: game-move-made
 *
 * Use cases:
 * - Game replay functionality
 * - Move analytics and pattern detection
 * - Anti-cheat detection
 * - Player behavior analysis
 *
 * This event is published every time a move is made (player or AI).
 * Consumers persist this to the game_move table for permanent storage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMoveEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique event identifier for idempotency
     */
    private UUID eventId;

    /**
     * Game identifier
     */
    private UUID gameId;

    /**
     * Move sequence number (1-indexed)
     */
    private int moveNumber;

    /**
     * Player type: HUMAN or AI
     */
    private String playerType;

    /**
     * Player ID (null if AI move)
     */
    private UUID playerId;

    /**
     * AI difficulty (null if human move)
     * Valid values: "EASY", "MEDIUM", "HARD", "EXPERT"
     */
    private String aiDifficulty;

    /**
     * Board X coordinate (0-14)
     */
    private int boardX;

    /**
     * Board Y coordinate (0-14)
     */
    private int boardY;

    /**
     * Stone color: BLACK or WHITE
     */
    private String stoneColor;

    /**
     * Time taken to make the move (milliseconds)
     */
    private Integer timeTakenMs;

    /**
     * Board state after this move (15x15 array)
     * Stored as JSON for event replay
     */
    private int[][] boardStateAfterMove;

    /**
     * Timestamp when move was made
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime timestamp;

    /**
     * Create event from move data
     */
    public static GameMoveEvent of(
            UUID gameId,
            int moveNumber,
            String playerType,
            UUID playerId,
            String aiDifficulty,
            int boardX,
            int boardY,
            String stoneColor,
            Integer timeTakenMs,
            int[][] boardState
    ) {
        return GameMoveEvent.builder()
                .eventId(UUID.randomUUID())
                .gameId(gameId)
                .moveNumber(moveNumber)
                .playerType(playerType)
                .playerId(playerId)
                .aiDifficulty(aiDifficulty)
                .boardX(boardX)
                .boardY(boardY)
                .stoneColor(stoneColor)
                .timeTakenMs(timeTakenMs)
                .boardStateAfterMove(boardState)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
