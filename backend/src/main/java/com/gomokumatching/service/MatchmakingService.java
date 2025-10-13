package com.gomokumatching.service;

import com.gomokumatching.exception.PlayerNotFoundException;
import com.gomokumatching.model.GameSession;
import com.gomokumatching.model.Player;
import com.gomokumatching.model.dto.matchmaking.MatchmakingQueueResponse;
import com.gomokumatching.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

/**
 * Matchmaking service for automatic player pairing.
 *
 * Responsibilities:
 * - Queue management (join, leave, status)
 * - Player matching (FIFO based on timestamp)
 * - Game creation for matched players
 *
 * Architecture:
 * - Uses Redis sorted set for queue (key: timestamp, value: playerId)
 * - FIFO matching: oldest players matched first
 * - Future: MMR-based matching by using MMR as score
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchmakingService {

    private final RedisService redisService;
    private final GameService gameService;
    private final PlayerRepository playerRepository;

    /**
     * Add player to matchmaking queue.
     *
     * If player is already in queue, returns current queue status.
     *
     * @param playerId Player ID
     * @return Queue response with position and status
     */
    public MatchmakingQueueResponse joinQueue(UUID playerId) {
        // validate player exists
        validatePlayerExists(playerId);

        // check if already in queue
        if (redisService.isPlayerInQueue(playerId)) {
            log.info("Player {} already in matchmaking queue", playerId);
            return getQueueStatus(playerId);
        }

        // add to queue with current timestamp
        double timestamp = System.currentTimeMillis();
        redisService.addToMatchmakingQueue(playerId, timestamp);

        long queueSize = redisService.getMatchmakingQueueSize();
        int position = calculateQueuePosition(playerId);
        LocalDateTime joinedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli((long) timestamp),
                ZoneId.systemDefault()
        );

        log.info("Player {} joined matchmaking queue. Position: {}, Total: {}",
                playerId, position, queueSize);

        return MatchmakingQueueResponse.joined(position, queueSize, joinedAt);
    }

    /**
     * Remove player from matchmaking queue.
     *
     * @param playerId Player ID
     * @return Queue response indicating success or not in queue
     */
    public MatchmakingQueueResponse leaveQueue(UUID playerId) {
        if (!redisService.isPlayerInQueue(playerId)) {
            log.debug("Player {} tried to leave queue but wasn't in it", playerId);
            return MatchmakingQueueResponse.notInQueue();
        }

        redisService.removeFromMatchmakingQueue(playerId);
        log.info("Player {} left matchmaking queue", playerId);

        return MatchmakingQueueResponse.left();
    }

    /**
     * Get current queue status for a player.
     *
     * @param playerId Player ID
     * @return Queue status with position and estimated wait time
     */
    public MatchmakingQueueResponse getQueueStatus(UUID playerId) {
        if (!redisService.isPlayerInQueue(playerId)) {
            return MatchmakingQueueResponse.notInQueue();
        }

        long queueSize = redisService.getMatchmakingQueueSize();
        int position = calculateQueuePosition(playerId);
        LocalDateTime joinedAt = getJoinTimestamp(playerId);

        return MatchmakingQueueResponse.alreadyInQueue(position, queueSize, joinedAt);
    }

    /**
     * Find and create a match from the queue.
     *
     * Pops the two oldest players from queue and creates a PvP game.
     * If game creation fails, players are re-added to queue.
     *
     * This method is called by the MatchmakingScheduler.
     *
     * @return Created game session, or null if not enough players
     */
    public GameSession findMatch() {
        long queueSize = redisService.getMatchmakingQueueSize();

        if (queueSize < 2) {
            log.debug("Not enough players in queue for matchmaking. Size: {}", queueSize);
            return null;
        }

        // pop two oldest players from queue
        UUID[] players = redisService.popTwoPlayersFromQueue();

        if (players == null || players.length < 2) {
            log.warn("Failed to pop 2 players from queue despite size >= 2");
            return null;
        }

        UUID player1Id = players[0];
        UUID player2Id = players[1];

        log.info("Matchmaking: Found match between {} and {}", player1Id, player2Id);

        try {
            // validate both players still exist and are active
            validatePlayerExists(player1Id);
            validatePlayerExists(player2Id);

            // create PvP game
            GameSession session = gameService.createPvPGame(player1Id, player2Id);

            log.info("✅ Match created successfully: game={}, player1={}, player2={}",
                    session.getGameId(), player1Id, player2Id);

            return session;

        } catch (PlayerNotFoundException e) {
            log.error("Player not found during matchmaking: {}", e.getMessage());
            // don't re-add invalid players to queue
            return null;

        } catch (Exception e) {
            log.error("Failed to create match between {} and {}: {}",
                    player1Id, player2Id, e.getMessage(), e);

            // re-add players to queue on failure
            double timestamp = System.currentTimeMillis();
            redisService.addToMatchmakingQueue(player1Id, timestamp);
            redisService.addToMatchmakingQueue(player2Id, timestamp + 1); // want some offset

            log.info("Re-added players {} and {} to matchmaking queue after failure",
                    player1Id, player2Id);

            return null;
        }
    }

    /**
     * Get total number of players in matchmaking queue.
     *
     * @return Queue size
     */
    public long getQueueSize() {
        return redisService.getMatchmakingQueueSize();
    }

    /**
     * Get all players currently in queue for debugging purposes.
     *
     * @return Set of player IDs in queue
     */
    public Set<String> getAllPlayersInQueue() {
        log.debug("Getting all players in queue (not yet implemented)");
        return Set.of();
    }

    // ===========================================
    // PRIVATE HELPER METHODS
    // ===========================================

    /**
     * Calculate player's position in queue (1-indexed).
     *
     * Position 1 = first in line, position 2 = second, etc.
     *
     * @param playerId Player ID
     * @return Queue position (1-indexed)
     */
    private int calculateQueuePosition(UUID playerId) {
        // redis ZRANK returns 0-indexed rank, we want 1-indexed position
        Long rank = redisService.redisTemplate.opsForZSet()
                .rank("matchmaking:queue", playerId.toString());

        if (rank == null) {
            return -1;
        }

        return rank.intValue() + 1;
    }

    /**
     * Get timestamp when player joined queue.
     *
     * @param playerId Player ID
     * @return Join timestamp, or null if not in queue
     */
    private LocalDateTime getJoinTimestamp(UUID playerId) {
        Double score = redisService.redisTemplate.opsForZSet()
                .score("matchmaking:queue", playerId.toString());

        if (score == null) {
            return null;
        }

        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(score.longValue()),
                ZoneId.systemDefault()
        );
    }

    /**
     * Validate that a player exists and is active.
     *
     * @param playerId Player ID
     * @throws PlayerNotFoundException if player doesn't exist or is inactive
     */
    private void validatePlayerExists(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));

        if (!player.isActive()) {
            throw new PlayerNotFoundException(playerId, "Player account is inactive");
        }
    }
}
