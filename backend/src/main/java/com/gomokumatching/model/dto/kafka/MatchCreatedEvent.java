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
 * Kafka event for match creation.
 *
 * Published to topic: match-created
 *
 * Use cases:
 * - Match history and analytics
 * - Matchmaking metrics (avg wait time, match rate)
 * - Player activity tracking
 * - System monitoring and alerting
 *
 * This event is published when:
 * - Two players are matched from matchmaking queue
 * - A PvP game is created manually
 * - A PvAI game is created
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCreatedEvent implements Serializable {

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
     * Game type: HUMAN_VS_HUMAN or HUMAN_VS_AI
     */
    private String gameType;

    /**
     * Player 1 ID (always human)
     */
    private UUID player1Id;

    /**
     * Player 2 ID (null for AI games)
     */
    private UUID player2Id;

    /**
     * AI difficulty (null for PvP games)
     * Valid values: "EASY", "MEDIUM", "HARD", "EXPERT"
     */
    private String aiDifficulty;

    /**
     * Match creation source: MATCHMAKING, DIRECT_CHALLENGE, or AI_GAME
     */
    private String matchSource;

    /**
     * Timestamp when match was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime timestamp;

    /**
     * Create event for matchmaking-created match
     */
    public static MatchCreatedEvent forMatchmaking(UUID gameId, UUID player1Id, UUID player2Id) {
        return MatchCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .gameId(gameId)
                .gameType("HUMAN_VS_HUMAN")
                .player1Id(player1Id)
                .player2Id(player2Id)
                .aiDifficulty(null)
                .matchSource("MATCHMAKING")
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Create event for PvP direct challenge
     */
    public static MatchCreatedEvent forDirectChallenge(UUID gameId, UUID player1Id, UUID player2Id) {
        return MatchCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .gameId(gameId)
                .gameType("HUMAN_VS_HUMAN")
                .player1Id(player1Id)
                .player2Id(player2Id)
                .aiDifficulty(null)
                .matchSource("DIRECT_CHALLENGE")
                .timestamp(OffsetDateTime.now())
                .build();
    }

    /**
     * Create event for PvAI game
     */
    public static MatchCreatedEvent forAIGame(UUID gameId, UUID playerId, String aiDifficulty) {
        return MatchCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .gameId(gameId)
                .gameType("HUMAN_VS_AI")
                .player1Id(playerId)
                .player2Id(null)
                .aiDifficulty(aiDifficulty)
                .matchSource("AI_GAME")
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
