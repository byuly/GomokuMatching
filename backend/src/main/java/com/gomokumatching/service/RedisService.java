package com.gomokumatching.service;

import com.gomokumatching.model.GameSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis service for managing game sessions and matchmaking queue.
 * Features:
 * - Game session CRUD with TTL (2 hours)
 * - Matchmaking queue management (sorted set)
 * - Key naming conventions for organization
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // using prefixes for debugging and organizing
    private static final String GAME_SESSION_PREFIX = "game:session:";
    private static final String MATCHMAKING_QUEUE_KEY = "matchmaking:queue";
    private static final long GAME_SESSION_TTL_HOURS = 2;

    /**
     * Save game session to Redis with 2-hour TTL
     */
    public void saveGameSession(GameSession session) {
        String key = getGameSessionKey(session.getGameId());
        redisTemplate.opsForValue().set(key, session, GAME_SESSION_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Saved game session: {} with TTL: {} hours", session.getGameId(), GAME_SESSION_TTL_HOURS);
    }

    /**
     * Retrieve game session from Redis
     */
    public GameSession getGameSession(UUID gameId) {
        String key = getGameSessionKey(gameId);
        Object obj = redisTemplate.opsForValue().get(key);

        if (obj == null) {
            log.warn("Game session not found: {}", gameId);
            return null;
        }

        return (GameSession) obj;
    }

    /**
     * Update existing game session (refresh TTL)
     */
    public void updateGameSession(GameSession session) {
        saveGameSession(session); // overwrite and refresh ttl
        log.debug("Updated game session: {}", session.getGameId());
    }

    /**
     * Delete game session from Redis
     */
    public void deleteGameSession(UUID gameId) {
        String key = getGameSessionKey(gameId);
        Boolean deleted = redisTemplate.delete(key);
        log.debug("Deleted game session: {} (success: {})", gameId, deleted);
    }

    /**
     * Check if game session exists
     */
    public boolean gameSessionExists(UUID gameId) {
        String key = getGameSessionKey(gameId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Extend TTL for active game
     */
    public void refreshGameSessionTTL(UUID gameId) {
        String key = getGameSessionKey(gameId);
        redisTemplate.expire(key, GAME_SESSION_TTL_HOURS, TimeUnit.HOURS);
        log.debug("Refreshed TTL for game session: {}", gameId);
    }

    // ===========================================
    // TOKEN BLACKLIST OPERATIONS
    // TODO: Implement token blacklisting for logout and refresh token rotation
    // ===========================================

    // TODO: Add token blacklist methods
    // public void blacklistToken(String token, long ttlSeconds) {
    //     String tokenId = extractTokenId(token);  // Extract jti claim or use hash
    //     redisTemplate.opsForValue().set("blacklist:token:" + tokenId, "revoked", ttlSeconds, TimeUnit.SECONDS);
    // }
    //
    // public boolean isTokenBlacklisted(String token) {
    //     String tokenId = extractTokenId(token);
    //     return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:token:" + tokenId));
    // }

    // ===========================================
    // for match making, use sorted sets in redis (NOT YET IMPLEMENTED)
    // TODO: implement matchmaking logic in backend
    // ===========================================

    /**
     * Add player to matchmaking queue with timestamp as score
     * Uses ZADD command: higher score = joined earlier (FIFO)
     */
    public void addToMatchmakingQueue(UUID playerId, double timestamp) {
        redisTemplate.opsForZSet().add(MATCHMAKING_QUEUE_KEY, playerId.toString(), timestamp);
        log.debug("Added player {} to matchmaking queue with timestamp: {}", playerId, timestamp);
    }

    /**
     * Remove player from matchmaking queue
     */
    public void removeFromMatchmakingQueue(UUID playerId) {
        Long removed = redisTemplate.opsForZSet().remove(MATCHMAKING_QUEUE_KEY, playerId.toString());
        log.debug("Removed player {} from matchmaking queue (count: {})", playerId, removed);
    }

    /**
     * Get queue size
     */
    public long getMatchmakingQueueSize() {
        Long size = redisTemplate.opsForZSet().size(MATCHMAKING_QUEUE_KEY);
        return size != null ? size : 0;
    }

    /**
     * Pop the two oldest players from queue (ZPOPMIN equivalent)
     * Returns null if less than 2 players in queue
     */
    public UUID[] popTwoPlayersFromQueue() {
        // pop 2 members with lowest scores (oldest timestamps)
        var popped = redisTemplate.opsForZSet().popMin(MATCHMAKING_QUEUE_KEY, 2);

        if (popped == null || popped.size() < 2) {
            log.debug("Not enough players in queue to form a match");
            return null;
        }

        UUID[] players = new UUID[2];
        int index = 0;
        for (var tuple : popped) {
            if (tuple.getValue() != null) {
                players[index++] = UUID.fromString(tuple.getValue().toString());
            }
        }

        if (index == 2) {
            log.info("Matched players: {} and {}", players[0], players[1]);
            return players;
        }

        return null;
    }

    /**
     * Check if player is in matchmaking queue
     */
    public boolean isPlayerInQueue(UUID playerId) {
        Double score = redisTemplate.opsForZSet().score(MATCHMAKING_QUEUE_KEY, playerId.toString());
        return score != null;
    }

    /**
     * Generate Redis key for game session
     */
    private String getGameSessionKey(UUID gameId) {
        return GAME_SESSION_PREFIX + gameId.toString();
    }
}
