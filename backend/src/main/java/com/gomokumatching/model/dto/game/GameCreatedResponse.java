package com.gomokumatching.model.dto.game;

import com.gomokumatching.model.GameSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO when a new game is created.
 *
 * Includes game ID and connection information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameCreatedResponse {

    /**
     * Unique game identifier
     */
    private UUID gameId;

    /**
     * Game type
     */
    private GameSession.GameType gameType;

    /**
     * Success message
     */
    private String message;

    /**
     * WebSocket subscription topic (for PvP games)
     * Example: "/topic/game/550e8400-e29b-41d4-a716-446655440000"
     */
    private String websocketTopic;

    /**
     * Create response for PvP game
     */
    public static GameCreatedResponse forPvPGame(UUID gameId) {
        return GameCreatedResponse.builder()
                .gameId(gameId)
                .gameType(GameSession.GameType.HUMAN_VS_HUMAN)
                .message("Game created successfully. Connect to WebSocket for real-time updates.")
                .websocketTopic("/topic/game/" + gameId)
                .build();
    }

    /**
     * Create response for PvAI game
     */
    public static GameCreatedResponse forPvAIGame(UUID gameId) {
        return GameCreatedResponse.builder()
                .gameId(gameId)
                .gameType(GameSession.GameType.HUMAN_VS_AI)
                .message("Game created successfully. Use REST API to make moves.")
                .websocketTopic(null) // No WebSocket for AI games
                .build();
    }
}
