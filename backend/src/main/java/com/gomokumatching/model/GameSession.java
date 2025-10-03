package com.gomokumatching.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * In-memory game session stored in Redis cache.
 *
 * Lifecycle:
 * - Created when game starts
 * - Updated after each move
 * - Deleted when game completes or TTL expires (2 hours)
 *
 * Board representation:
 * - 15x15 int array
 * - 0 = empty, 1 = player1/black, 2 = player2/white/AI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique game identifier (maps to Game.gameId in PostgreSQL)
     */
    private UUID gameId;

    /**
     * Game type: HUMAN_VS_HUMAN or HUMAN_VS_AI
     */
    private GameType gameType;

    /**
     * Current game status
     */
    private GameStatus status;

    /**
     * Player 1 ID (always human)
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
     * 15x15 game board
     * 0 = empty position
     * 1 = player 1 stone (BLACK)
     * 2 = player 2/AI stone (WHITE)
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
     * Winner type (null if game in progress)
     */
    private String winnerType;

    /**
     * Winner ID (null if game in progress, draw, or AI won)
     */
    private UUID winnerId;

    /**
     * Game start timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    /**
     * Last activity timestamp (for TTL management)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActivity;

    /**
     * Game end timestamp (null if in progress)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endedAt;

    // ===========================================
    // GAME LOGIC HELPER METHODS
    // ===========================================

    /**
     * Initialize a new 15x15 empty board
     */
    public static int[][] createEmptyBoard() {
        return new int[15][15];
    }

    /**
     * Validate if a move is legal
     */
    public boolean isValidMove(int row, int col) {
        // Check bounds
        if (row < 0 || row >= 15 || col < 0 || col >= 15) {
            return false;
        }
        // Check if position is empty
        return board[row][col] == 0;
    }

    /**
     * Make a move on the board
     */
    public void makeMove(int row, int col, int player) {
        if (!isValidMove(row, col)) {
            throw new IllegalArgumentException("Invalid move: position (" + row + ", " + col + ") is occupied or out of bounds");
        }
        board[row][col] = player;
        moveCount++;
        lastActivity = LocalDateTime.now();
    }

    /**
     * Switch to next player
     */
    public void switchPlayer() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
    }

    /**
     * Check if board is full (draw condition)
     */
    public boolean isBoardFull() {
        return moveCount >= 225; // 15 * 15
    }

    /**
     * Update last activity (for TTL tracking)
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    // ===========================================
    // ENUMS
    // ===========================================

    public enum GameType {
        HUMAN_VS_HUMAN,
        HUMAN_VS_AI
    }

    public enum GameStatus {
        WAITING,        // Waiting for players
        IN_PROGRESS,    // Game actively being played
        COMPLETED,      // Game finished normally
        ABANDONED       // Player disconnected/forfeited
    }
}
