package com.gomokumatching.model.dto.game;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gomokumatching.model.GameSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Complete game state response DTO.
 *
 * Sent to clients via REST or WebSocket with full game information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {

    /**
     * Unique game identifier
     */
    private UUID gameId;

    /**
     * Game type (HUMAN_VS_HUMAN or HUMAN_VS_AI)
     */
    private GameSession.GameType gameType;

    /**
     * Current game status
     */
    private GameSession.GameStatus status;

    /**
     * Player 1 ID
     */
    private UUID player1Id;

    /**
     * Player 2 ID (null for AI games)
     */
    private UUID player2Id;

    /**
     * AI opponent ID (null for PvP games)
     */
    private UUID aiOpponentId;

    /**
     * 15x15 game board (0=empty, 1=player1, 2=player2/AI)
     */
    private int[][] board;

    /**
     * Current player turn (1 or 2)
     */
    private int currentPlayer;

    /**
     * Total number of moves made
     */
    private int moveCount;

    /**
     * Winner type (PLAYER1, PLAYER2, AI, DRAW, NONE)
     */
    private String winnerType;

    /**
     * Winner player ID (null if AI won, draw, or game in progress)
     */
    private UUID winnerId;

    /**
     * Game start timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    /**
     * Game end timestamp (null if in progress)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endedAt;

    /**
     * Last activity timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActivity;

    /**
     * Recent moves (last 5 for UI display, optional)
     */
    private List<MoveDTO> recentMoves;

    /**
     * Create from GameSession
     */
    public static GameStateResponse fromGameSession(GameSession session) {
        return GameStateResponse.builder()
                .gameId(session.getGameId())
                .gameType(session.getGameType())
                .status(session.getStatus())
                .player1Id(session.getPlayer1Id())
                .player2Id(session.getPlayer2Id())
                .aiOpponentId(session.getAiOpponentId())
                .board(session.getBoard())
                .currentPlayer(session.getCurrentPlayer())
                .moveCount(session.getMoveCount())
                .winnerType(session.getWinnerType())
                .winnerId(session.getWinnerId())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .lastActivity(session.getLastActivity())
                .recentMoves(null) // Can be populated from database if needed
                .build();
    }
}
