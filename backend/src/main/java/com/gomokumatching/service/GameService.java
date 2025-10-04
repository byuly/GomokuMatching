package com.gomokumatching.service;

import com.gomokumatching.model.Game;
import com.gomokumatching.model.GameSession;
import com.gomokumatching.model.Player;
import com.gomokumatching.model.AIOpponent;
import com.gomokumatching.model.enums.GameStatusEnum;
import com.gomokumatching.model.enums.GameTypeEnum;
import com.gomokumatching.model.enums.WinnerTypeEnum;
import com.gomokumatching.repository.GameRepository;
import com.gomokumatching.repository.PlayerRepository;
import com.gomokumatching.repository.AIOpponentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final AIOpponentRepository aiOpponentRepository;
    private final ObjectMapper objectMapper;

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
        else if (session.checkIfBoardFull()) {
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

        // Save forfeited game to PostgreSQL
        saveCompletedGameToDatabase(session);

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

        // Save completed game to PostgreSQL
        saveCompletedGameToDatabase(session);
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

        // Save completed game to PostgreSQL
        saveCompletedGameToDatabase(session);
    }

    /**
     * Save completed game session from Redis to PostgreSQL for permanent storage.
     * Converts in-memory GameSession to database Game entity.
     */
    private void saveCompletedGameToDatabase(GameSession session) {
        try {
            Game game = new Game();
            game.setGameId(session.getGameId());

            // Set game type
            game.setGameType(session.getGameType() == GameSession.GameType.HUMAN_VS_HUMAN
                    ? GameTypeEnum.HUMAN_VS_HUMAN
                    : GameTypeEnum.HUMAN_VS_AI);

            game.setGameStatus(GameStatusEnum.COMPLETED);

            // Set players
            Player player1 = playerRepository.findById(session.getPlayer1Id())
                    .orElseThrow(() -> new IllegalStateException("Player1 not found: " + session.getPlayer1Id()));
            game.setPlayer1(player1);

            if (session.getPlayer2Id() != null) {
                Player player2 = playerRepository.findById(session.getPlayer2Id())
                        .orElseThrow(() -> new IllegalStateException("Player2 not found: " + session.getPlayer2Id()));
                game.setPlayer2(player2);
            }

            if (session.getAiOpponentId() != null) {
                AIOpponent aiOpponent = aiOpponentRepository.findById(session.getAiOpponentId())
                        .orElseThrow(() -> new IllegalStateException("AI opponent not found: " + session.getAiOpponentId()));
                game.setAiOpponent(aiOpponent);
            }

            // Set winner
            String winnerTypeStr = session.getWinnerType();
            if ("PLAYER1".equals(winnerTypeStr)) {
                game.setWinnerType(WinnerTypeEnum.PLAYER1);
                game.setWinner(player1);
            } else if ("PLAYER2".equals(winnerTypeStr)) {
                game.setWinnerType(WinnerTypeEnum.PLAYER2);
                if (session.getWinnerId() != null) {
                    Player winner = playerRepository.findById(session.getWinnerId())
                            .orElseThrow(() -> new IllegalStateException("Winner not found: " + session.getWinnerId()));
                    game.setWinner(winner);
                }
            } else if ("AI".equals(winnerTypeStr)) {
                game.setWinnerType(WinnerTypeEnum.AI);
                game.setWinner(null);
            } else if ("DRAW".equals(winnerTypeStr)) {
                game.setWinnerType(WinnerTypeEnum.DRAW);
                game.setWinner(null);
            } else {
                game.setWinnerType(WinnerTypeEnum.NONE);
                game.setWinner(null);
            }

            game.setTotalMoves(session.getMoveCount());

            // Convert LocalDateTime to OffsetDateTime
            game.setStartedAt(session.getStartedAt().atOffset(ZoneOffset.UTC));
            game.setEndedAt(session.getEndedAt().atOffset(ZoneOffset.UTC));

            // Calculate duration
            long durationSeconds = java.time.Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds();
            game.setGameDurationSeconds((int) durationSeconds);

            // Store final board state as JSON
            String boardJson = objectMapper.writeValueAsString(session.getBoard());
            game.setFinalBoardState(boardJson);

            // Store move sequence as JSON (if available)
            if (session.getMoveHistory() != null && !session.getMoveHistory().isEmpty()) {
                String moveSequenceJson = objectMapper.writeValueAsString(session.getMoveHistory());
                game.setMoveSequence(moveSequenceJson);
            }

            // Save to database
            gameRepository.save(game);
            log.info("✅ Game {} saved to PostgreSQL: {} winner in {} moves",
                    session.getGameId(), winnerTypeStr, session.getMoveCount());

        } catch (Exception e) {
            log.error("❌ Failed to save game {} to PostgreSQL: {}", session.getGameId(), e.getMessage(), e);
        }
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
