package com.gomokumatching.model.dto.game;

import com.gomokumatching.model.GameSession;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new game.
 *
 * Business Rule: Exactly ONE of player2Id or aiOpponentId must be set
 * - player2Id set → Player vs Player game
 * - aiOpponentId set → Player vs AI game
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameRequest {

    /**
     * Game type (HUMAN_VS_HUMAN or HUMAN_VS_AI)
     */
    @NotNull(message = "Game type is required")
    private GameSession.GameType gameType;

    /**
     * Player 2 ID (for PvP games only, must be null for AI games)
     */
    private UUID player2Id;

    /**
     * AI opponent ID (for PvAI games only, must be null for PvP games)
     */
    private UUID aiOpponentId;

    /**
     * Validate business rule: exactly one opponent type must be specified
     */
    public boolean isValid() {
        boolean hasPlayer2 = player2Id != null;
        boolean hasAI = aiOpponentId != null;

        // XOR: exactly one must be set
        return hasPlayer2 ^ hasAI;
    }

    /**
     * Validate game type matches opponent type
     */
    public boolean isGameTypeConsistent() {
        if (gameType == GameSession.GameType.HUMAN_VS_HUMAN) {
            return player2Id != null && aiOpponentId == null;
        } else if (gameType == GameSession.GameType.HUMAN_VS_AI) {
            return aiOpponentId != null && player2Id == null;
        }
        return false;
    }
}
