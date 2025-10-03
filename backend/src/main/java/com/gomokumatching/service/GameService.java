package com.gomokumatching.service;

import com.gomokumatching.model.GameSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core game logic service for Gomoku.
 *
 * Responsibilities:
 * - Create new games
 * - Process moves and validate them
 * - Detect win conditions (5 in a row)
 * - Manage game state in Redis
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {

    private final RedisService redisService;

    private static final int BOARD_SIZE = 15;
    private static final int WIN_CONDITION = 5; // 5 stones in a row

    // ===========================================
    // GAME CREATION
    // ===========================================

    /**
     * Create a new Player vs Player game
     */
    public GameSession createPvPGame(UUID player1Id, UUID player2Id) {
        GameSession session = new GameSession();
        session.setGameId(UUID.randomUUID());
        session.setGameType(GameSession.GameType.HUMAN_VS_HUMAN);
        session.setStatus(GameSession.GameStatus.IN_PROGRESS);
        session.setPlayer1Id(player1Id);
        session.setPlayer2Id(player2Id);
        session.setAiOpponentId(null);
        session.setBoard(GameSession.createEmptyBoard());
        session.setCurrentPlayer(1); // Player 1 (BLACK) starts
        session.setMoveCount(0);
        session.setStartedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());

        redisService.saveGameSession(session);
        log.info("Created PvP game: {} between {} and {}", session.getGameId(), player1Id, player2Id);

        return session;
    }

    /**
     * Create a new Player vs AI game
     */
    public GameSession createPvAIGame(UUID playerId, UUID aiOpponentId) {
        GameSession session = new GameSession();
        session.setGameId(UUID.randomUUID());
        session.setGameType(GameSession.GameType.HUMAN_VS_AI);
        session.setStatus(GameSession.GameStatus.IN_PROGRESS);
        session.setPlayer1Id(playerId);
        session.setPlayer2Id(null);
        session.setAiOpponentId(aiOpponentId);
        session.setBoard(GameSession.createEmptyBoard());
        session.setCurrentPlayer(1); // Player (BLACK) starts
        session.setMoveCount(0);
        session.setStartedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());

        redisService.saveGameSession(session);
        log.info("Created PvAI game: {} between player {} and AI {}", session.getGameId(), playerId, aiOpponentId);

        return session;
    }

    // ===========================================
    // MOVE PROCESSING
    // ===========================================

    /**
     * Process a player move
     *
     * @return Updated game session
     * @throws IllegalArgumentException if move is invalid
     * @throws IllegalStateException if game is not in progress
     */
    public GameSession processMove(UUID gameId, UUID playerId, int row, int col) {
        // Get game session from Redis
        GameSession session = redisService.getGameSession(gameId);
        if (session == null) {
            throw new IllegalStateException("Game session not found: " + gameId);
        }

        // Validate game state
        if (session.getStatus() != GameSession.GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }

        // Determine which player this is (1 or 2)
        int playerNumber = getPlayerNumber(session, playerId);
        if (playerNumber != session.getCurrentPlayer()) {
            throw new IllegalStateException("Not your turn");
        }

        // Validate and make move
        if (!session.isValidMove(row, col)) {
            throw new IllegalArgumentException("Invalid move at position (" + row + ", " + col + ")");
        }

        session.makeMove(row, col, playerNumber);
        log.debug("Player {} made move at ({}, {}) in game {}", playerId, row, col, gameId);

        // Check win condition
        if (checkWinCondition(session.getBoard(), row, col, playerNumber)) {
            handleGameWin(session, playerId, playerNumber);
        }
        // Check draw condition
        else if (session.isBoardFull()) {
            handleGameDraw(session);
        }
        // Continue game - switch player
        else {
            session.switchPlayer();
        }

        // Update Redis
        redisService.updateGameSession(session);

        return session;
    }

    /**
     * Get game state from Redis
     */
    public GameSession getGameState(UUID gameId) {
        return redisService.getGameSession(gameId);
    }

    /**
     * Forfeit game (player gives up)
     */
    public GameSession forfeitGame(UUID gameId, UUID playerId) {
        GameSession session = redisService.getGameSession(gameId);
        if (session == null) {
            throw new IllegalStateException("Game session not found: " + gameId);
        }

        // Determine opponent
        UUID opponentId;
        String winnerType;

        if (playerId.equals(session.getPlayer1Id())) {
            opponentId = session.getPlayer2Id();
            winnerType = (session.getGameType() == GameSession.GameType.HUMAN_VS_AI) ? "AI" : "PLAYER2";
        } else {
            opponentId = session.getPlayer1Id();
            winnerType = "PLAYER1";
        }

        session.setStatus(GameSession.GameStatus.ABANDONED);
        session.setWinnerType(winnerType);
        session.setWinnerId(winnerType.equals("AI") ? null : opponentId);
        session.setEndedAt(LocalDateTime.now());

        redisService.updateGameSession(session);
        log.info("Game {} forfeited by player {}", gameId, playerId);

        return session;
    }

    // ===========================================
    // WIN CONDITION DETECTION
    // ===========================================

    /**
     * Check if the last move resulted in a win (5 in a row).
     * Only checks around the last placed stone for efficiency.
     *
     * Checks 4 directions:
     * 1. Horizontal (—)
     * 2. Vertical (|)
     * 3. Diagonal (\)
     * 4. Anti-diagonal (/)
     *
     * @param board Current board state
     * @param row Last move row
     * @param col Last move column
     * @param player Player number (1 or 2)
     * @return true if player has 5 in a row
     */
    public boolean checkWinCondition(int[][] board, int row, int col, int player) {
        // Check all 4 directions
        return checkDirection(board, row, col, player, 0, 1)  ||  // Horizontal
               checkDirection(board, row, col, player, 1, 0)  ||  // Vertical
               checkDirection(board, row, col, player, 1, 1)  ||  // Diagonal \
               checkDirection(board, row, col, player, 1, -1);    // Anti-diagonal /
    }

    /**
     * Check if there are 5 consecutive stones in a specific direction.
     *
     * @param board Game board
     * @param row Starting row
     * @param col Starting column
     * @param player Player number
     * @param dRow Row direction (-1, 0, 1)
     * @param dCol Column direction (-1, 0, 1)
     * @return true if 5 in a row found
     */
    private boolean checkDirection(int[][] board, int row, int col, int player, int dRow, int dCol) {
        int count = 1; // Count the stone just placed

        // Check positive direction
        count += countStones(board, row, col, player, dRow, dCol);

        // Check negative direction
        count += countStones(board, row, col, player, -dRow, -dCol);

        return count >= WIN_CONDITION;
    }

    /**
     * Count consecutive stones in one direction.
     *
     * @param board Game board
     * @param row Starting row
     * @param col Starting column
     * @param player Player number
     * @param dRow Row direction
     * @param dCol Column direction
     * @return Number of consecutive stones found
     */
    private int countStones(int[][] board, int row, int col, int player, int dRow, int dCol) {
        int count = 0;
        int r = row + dRow;
        int c = col + dCol;

        while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == player) {
            count++;
            r += dRow;
            c += dCol;
        }

        return count;
    }

    // ===========================================
    // GAME STATE MANAGEMENT
    // ===========================================

    /**
     * Handle game win
     */
    private void handleGameWin(GameSession session, UUID winnerId, int playerNumber) {
        session.setStatus(GameSession.GameStatus.COMPLETED);

        if (session.getGameType() == GameSession.GameType.HUMAN_VS_AI) {
            session.setWinnerType(playerNumber == 1 ? "PLAYER1" : "AI");
            session.setWinnerId(playerNumber == 1 ? winnerId : null);
        } else {
            session.setWinnerType(playerNumber == 1 ? "PLAYER1" : "PLAYER2");
            session.setWinnerId(winnerId);
        }

        session.setEndedAt(LocalDateTime.now());
        log.info("Game {} won by player {} ({})", session.getGameId(), winnerId, session.getWinnerType());
    }

    /**
     * Handle game draw
     */
    private void handleGameDraw(GameSession session) {
        session.setStatus(GameSession.GameStatus.COMPLETED);
        session.setWinnerType("DRAW");
        session.setWinnerId(null);
        session.setEndedAt(LocalDateTime.now());
        log.info("Game {} ended in a draw", session.getGameId());
    }

    /**
     * Determine which player number (1 or 2) this player ID represents
     */
    private int getPlayerNumber(GameSession session, UUID playerId) {
        if (playerId.equals(session.getPlayer1Id())) {
            return 1;
        } else if (playerId.equals(session.getPlayer2Id())) {
            return 2;
        } else {
            throw new IllegalArgumentException("Player " + playerId + " is not in this game");
        }
    }
}
