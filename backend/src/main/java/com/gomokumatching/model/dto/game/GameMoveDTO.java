package com.gomokumatching.model.dto.game;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gomokumatching.model.GameMove;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for game move history.
 *
 * Used by GET /api/game/{gameId}/moves endpoint to return move-by-move
 * game replay data.
 *
 * Includes board state after each move for complete replay functionality.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMoveDTO {

    /**
     * Move sequence number (1, 2, 3...)
     */
    private int moveNumber;

    /**
     * Player type: HUMAN or AI
     */
    private String playerType;

    /**
     * Player ID (null for AI moves)
     */
    private UUID playerId;

    /**
     * AI difficulty (null for human moves)
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
     * Time taken to make this move (milliseconds)
     */
    private Integer timeTakenMs;

    /**
     * Board state after this move (15x15 array)
     * Enables step-by-step replay
     */
    private String boardStateAfterMove;

    /**
     * Timestamp when move was made
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime moveTimestamp;

    /**
     * Convert GameMove entity to DTO.
     *
     * @param move GameMove entity from database
     * @return GameMoveDTO for API response
     */
    public static GameMoveDTO fromEntity(GameMove move) {
        return GameMoveDTO.builder()
                .moveNumber(move.getMoveNumber())
                .playerType(move.getPlayerType().name())
                .playerId(move.getPlayer() != null ? move.getPlayer().getPlayerId() : null)
                .aiDifficulty(move.getAiDifficulty())
                .boardX(move.getBoardX())
                .boardY(move.getBoardY())
                .stoneColor(move.getStoneColor().name())
                .timeTakenMs(move.getTimeTakenMs())
                .boardStateAfterMove(move.getBoardStateAfterMove())
                .moveTimestamp(move.getMoveTimestamp())
                .build();
    }
}
