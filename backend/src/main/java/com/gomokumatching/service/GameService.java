package com.gomokumatching.service;

import com.gomokumatching.model.Game;
import com.gomokumatching.model.GameMove;
import com.gomokumatching.model.GameSession;
import com.gomokumatching.model.Player;
import com.gomokumatching.model.dto.kafka.GameMoveEvent;
import com.gomokumatching.model.enums.GameStatusEnum;
import com.gomokumatching.model.enums.GameTypeEnum;
import com.gomokumatching.model.enums.PlayerTypeEnum;
import com.gomokumatching.model.enums.StoneColorEnum;
import com.gomokumatching.model.enums.WinnerTypeEnum;
import com.gomokumatching.repository.GameMoveRepository;
import com.gomokumatching.repository.GameRepository;
import com.gomokumatching.repository.PlayerRepository;
import com.gomokumatching.service.kafka.GameEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
 * - Publish game events to Kafka
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {

    private final RedisService redisService;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;
    private final GameEventProducer gameEventProducer;
    private final PlayerStatsService playerStatsService;
    private final GameMoveRepository gameMoveRepository;

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
        session.setAiDifficulty(null);
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
    public GameSession createPvAIGame(UUID playerId, String aiDifficulty) {
        GameSession session = new GameSession();
        session.setGameId(UUID.randomUUID());
        session.setGameType(GameSession.GameType.HUMAN_VS_AI);
        session.setStatus(GameSession.GameStatus.IN_PROGRESS);
        session.setPlayer1Id(playerId);
        session.setPlayer2Id(null);
        session.setAiDifficulty(aiDifficulty);
        session.setBoard(GameSession.createEmptyBoard());
        session.setCurrentPlayer(1); // Player (BLACK) starts
        session.setMoveCount(0);
        session.setStartedAt(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());

        redisService.saveGameSession(session);
        log.info("Created PvAI game: {} between player {} and AI difficulty {}", session.getGameId(), playerId, aiDifficulty);

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
        // first get game session from redis
        GameSession session = redisService.getGameSession(gameId);
        if (session == null) {
            throw new IllegalStateException("Game session not found: " + gameId);
        }

        // validating game state
        if (session.getStatus() != GameSession.GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game is not in progress");
        }

        // determining player 1 and 2, for turn checking
        int playerNumber = getPlayerNumber(session, playerId);
        if (playerNumber != session.getCurrentPlayer()) {
            throw new IllegalStateException("Not your turn");
        }

        // validate the move
        if (!session.isValidMove(row, col)) {
            throw new IllegalArgumentException("Invalid move at position (" + row + ", " + col + ")");
        }

        session.makeMove(row, col, playerNumber);
        log.debug("{} made move at ({}, {}) in game {}",
                playerId == null ? "AI" : "Player " + playerId, row, col, gameId);

        // async publish move event to Kafka
        publishMoveEvent(session, playerId, row, col, playerNumber);

        // check win condition each move, but only around the rock placed, not the whole board
        if (checkWinCondition(session.getBoard(), row, col, playerNumber)) {
            handleGameWin(session, playerId, playerNumber);
        }

        // possible to draw in this game, so want to end as draw when this happens
        else if (session.checkIfBoardFull()) {
            handleGameDraw(session);
        }

        // continue the game after processing move, with switching curr player
        else {
            session.switchPlayer();
        }

        // updating redis with new game session
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

        // saving forfeited game like game is completed
        saveCompletedGameToDatabase(session);

        // update player stats
        UUID winnerId = winnerType.equals("AI") ? null : opponentId;
        playerStatsService.updateStatsAfterGame(
                session.getPlayer1Id(),
                session.getPlayer2Id(),
                winnerId,
                false
        );

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
        int count = 1; // the stone currently placed

        // positive direction
        count += countStones(board, row, col, player, dRow, dCol);

        // negative direction
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
    // game state management helpers
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

        saveCompletedGameToDatabase(session);

        // update player stats
        playerStatsService.updateStatsAfterGame(
                session.getPlayer1Id(),
                session.getPlayer2Id(),
                winnerId,
                false // not a draw
        );
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

        saveCompletedGameToDatabase(session);

        // update player stats
        playerStatsService.updateStatsAfterGame(
                session.getPlayer1Id(),
                session.getPlayer2Id(),
                null, // no winner
                true // is a draw
        );
    }

    /**
     * Save completed game session from Redis to PostgreSQL for permanent storage.
     * Converts in-memory GameSession to database Game entity.
     */
    private void saveCompletedGameToDatabase(GameSession session) {
        try {
            Game game = new Game();
            game.setGameId(session.getGameId());

            game.setGameType(session.getGameType() == GameSession.GameType.HUMAN_VS_HUMAN
                    ? GameTypeEnum.HUMAN_VS_HUMAN
                    : GameTypeEnum.HUMAN_VS_AI);

            // map session status to database enum
            if (session.getStatus() == GameSession.GameStatus.ABANDONED) {
                game.setGameStatus(GameStatusEnum.ABANDONED);
            } else {
                game.setGameStatus(GameStatusEnum.COMPLETED);
            }

            Player player1 = playerRepository.findById(session.getPlayer1Id())
                    .orElseThrow(() -> new IllegalStateException("Player1 not found: " + session.getPlayer1Id()));
            game.setPlayer1(player1);

            if (session.getPlayer2Id() != null) {
                Player player2 = playerRepository.findById(session.getPlayer2Id())
                        .orElseThrow(() -> new IllegalStateException("Player2 not found: " + session.getPlayer2Id()));
                game.setPlayer2(player2);
            }

            if (session.getAiDifficulty() != null) {
                game.setAiDifficulty(session.getAiDifficulty());
            }

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

            game.setStartedAt(session.getStartedAt().atOffset(ZoneOffset.UTC));
            game.setEndedAt(session.getEndedAt().atOffset(ZoneOffset.UTC));

            long durationSeconds = java.time.Duration.between(session.getStartedAt(), session.getEndedAt()).getSeconds();
            game.setGameDurationSeconds((int) durationSeconds);

            // we are storing final board state as JSON
            String boardJson = objectMapper.writeValueAsString(session.getBoard());
            game.setFinalBoardState(boardJson);

            // sequence of moves here for possible replay feature
            if (session.getMoveHistory() != null && !session.getMoveHistory().isEmpty()) {
                String moveSequenceJson = objectMapper.writeValueAsString(session.getMoveHistory());
                game.setMoveSequence(moveSequenceJson);
            }

            gameRepository.save(game); // to DB

            log.info("✅ Game {} saved to PostgreSQL: {} winner in {} moves",
                    session.getGameId(), winnerTypeStr, session.getMoveCount());

            // Persist move history from Redis to database
            saveMoveHistoryToDatabase(game, session);

        } catch (Exception e) {
            log.error("❌ Failed to save game {} to PostgreSQL: {}", session.getGameId(), e.getMessage(), e);
        }
    }

    /**
     * Save move history from GameSession to database.
     *
     * This method is called when a game completes to persist all moves from the
     * in-memory GameSession to the game_move table for replay functionality.
     *
     * Works in conjunction with Kafka GameMovesConsumer:
     * - Kafka consumer provides real-time move persistence (if game already in DB)
     * - This method ensures all moves are persisted when game completes
     * - Handles duplicates gracefully via unique constraints
     *
     * @param game The saved Game entity (must exist in database)
     * @param session The GameSession from Redis with move history
     */
    private void saveMoveHistoryToDatabase(Game game, GameSession session) {
        if (session.getMoveHistory() == null || session.getMoveHistory().isEmpty()) {
            log.debug("No move history to persist for game {}", session.getGameId());
            return;
        }

        try {
            int moveNumber = 1; // Move numbers start at 1
            for (int[] move : session.getMoveHistory()) {
                try {
                    // Parse move array: [row, col, playerNumber]
                    int row = move[0];
                    int col = move[1];
                    int playerNumber = move[2];

                    // Check if move already exists (Kafka consumer may have saved it)
                    GameMove existingMove = gameMoveRepository.findByGameIdAndMoveNumber(
                            session.getGameId(),
                            moveNumber
                    );

                    if (existingMove != null) {
                        log.debug("Move {} already persisted for game {} (via Kafka), skipping",
                                moveNumber, session.getGameId());
                        moveNumber++;
                        continue;
                    }

                    // Create new GameMove entity
                    GameMove gameMove = new GameMove();
                    gameMove.setGame(game);
                    gameMove.setMoveNumber(moveNumber);
                    gameMove.setBoardX(row);
                    gameMove.setBoardY(col);

                    // Determine player type and set player reference
                    // Player 1 is always human, Player 2 could be human or AI
                    final UUID playerId;
                    if (playerNumber == 1) {
                        playerId = session.getPlayer1Id();
                    } else if (playerNumber == 2 && session.getPlayer2Id() != null) {
                        playerId = session.getPlayer2Id();
                    } else {
                        playerId = null;
                    }

                    if (playerId != null) {
                        // Human player move
                        gameMove.setPlayerType(PlayerTypeEnum.HUMAN);
                        Player player = playerRepository.findById(playerId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Player not found: " + playerId));
                        gameMove.setPlayer(player);
                        gameMove.setAiDifficulty(null);
                    } else {
                        // AI move (player 2 in AI game)
                        gameMove.setPlayerType(PlayerTypeEnum.AI);
                        gameMove.setPlayer(null);
                        gameMove.setAiDifficulty(session.getAiDifficulty());
                    }

                    // Set stone color (player 1 = BLACK, player 2 = WHITE)
                    gameMove.setStoneColor(playerNumber == 1
                            ? StoneColorEnum.BLACK
                            : StoneColorEnum.WHITE);

                    // Store board state after move as JSON (we don't track this in moveHistory)
                    // For now, set to null - could reconstruct if needed
                    gameMove.setBoardStateAfterMove(null);

                    // Time taken is not tracked in current implementation
                    gameMove.setTimeTakenMs(null);

                    gameMoveRepository.save(gameMove);

                    moveNumber++;

                } catch (Exception e) {
                    // Log but continue with other moves - duplicates will fail unique constraint
                    log.warn("Failed to save move {} for game {}: {}",
                            moveNumber, session.getGameId(), e.getMessage());
                    moveNumber++;
                }
            }

            log.info("✅ Persisted {} moves from move history for game {}",
                    session.getMoveHistory().size(), session.getGameId());

        } catch (Exception e) {
            // Don't throw - move persistence failure shouldn't break game completion
            log.error("❌ Failed to save move history for game {}: {}",
                    session.getGameId(), e.getMessage(), e);
        }
    }

    /**
     * Determine which player number (1 or 2) this player ID represents
     * For AI games, playerId can be null (representing the AI player 2)
     */
    private int getPlayerNumber(GameSession session, UUID playerId) {
        if (playerId == null && session.getGameType() == GameSession.GameType.HUMAN_VS_AI) {
            // AI is always player 2 in PvAI games
            return 2;
        } else if (playerId != null && playerId.equals(session.getPlayer1Id())) {
            return 1;
        } else if (playerId != null && playerId.equals(session.getPlayer2Id())) {
            return 2;
        } else {
            throw new IllegalArgumentException("Player " + playerId + " is not in this game");
        }
    }

    /**
     * Publish game move event to Kafka.
     *
     * Creates and publishes a GameMoveEvent for analytics and replay.
     * Async execution via GameEventProducer ensures this doesn't block game flow.
     *
     * @param session Current game session (after move is made)
     * @param playerId Player ID (null for AI)
     * @param row Board row (0-14)
     * @param col Board column (0-14)
     * @param playerNumber Player number (1 or 2)
     */
    private void publishMoveEvent(GameSession session, UUID playerId, int row, int col, int playerNumber) {
        try {
            String playerType = (playerId == null) ? "AI" : "HUMAN";
            String stoneColor = (playerNumber == 1) ? "BLACK" : "WHITE";

            GameMoveEvent event = GameMoveEvent.of(
                    session.getGameId(),
                    session.getMoveCount(),
                    playerType,
                    playerId,
                    session.getAiDifficulty(),
                    row,
                    col,
                    stoneColor,
                    null, // timeTakenMs - maybe we could implement this by tracking start time of move? would be cool
                    session.getBoard()
            );

            gameEventProducer.publishGameMove(event);

        } catch (Exception e) {
            // log error but don't throw - Kafka failures shouldn't break game flow
            log.error("Failed to publish move event for game {}: {}", session.getGameId(), e.getMessage());
        }
    }
}
