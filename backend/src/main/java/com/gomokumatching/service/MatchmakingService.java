package com.gomokumatching.service;

import com.gomokumatching.exception.PlayerNotFoundException;
import com.gomokumatching.model.Player;
import com.gomokumatching.model.dto.matchmaking.MatchmakingQueueResponse;
import com.gomokumatching.repository.PlayerRepository;
import com.gomokumatching.service.kafka.QueueEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    private final QueueEventProducer queueEventProducer;
    private final PlayerRepository playerRepository;

    public MatchmakingQueueResponse joinQueue(UUID playerId) {
        validatePlayerExists(playerId);
        queueEventProducer.publishPlayerJoined(playerId);

        log.info("Player {} joined matchmaking queue", playerId);
        return MatchmakingQueueResponse.joined(0, 0, LocalDateTime.now());
    }

    public MatchmakingQueueResponse leaveQueue(UUID playerId) {
        queueEventProducer.publishPlayerLeft(playerId);
        log.info("Player {} left matchmaking queue", playerId);
        return MatchmakingQueueResponse.left();
    }

    // kafka streams maintains queue state, so we can't provide accurate status here
    // frontend should rely on websocket notifications for match-found events
    public MatchmakingQueueResponse getQueueStatus(UUID playerId) {
        return MatchmakingQueueResponse.notInQueue();
    }

    private void validatePlayerExists(UUID playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));

        if (!player.isActive()) {
            throw new PlayerNotFoundException(playerId, "Player account is inactive");
        }
    }
}
