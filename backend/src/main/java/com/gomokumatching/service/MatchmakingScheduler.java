package com.gomokumatching.service;

import com.gomokumatching.model.GameSession;
import com.gomokumatching.model.dto.kafka.MatchCreatedEvent;
import com.gomokumatching.service.kafka.GameEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Scheduled task for automatic player matchmaking.
 *
 * Polls the matchmaking queue at regular intervals and creates games
 * when two or more players are waiting.
 *
 * Architecture:
 * - Fixed delay scheduling (runs every 2 seconds after previous execution completes)
 * - Processes all possible matches in a single run
 * - Sends WebSocket notifications to matched players
 * - Publishes match-created events to Kafka
 * - Handles errors gracefully without stopping scheduler
 *
 * Configuration:
 * - Poll interval: 2000ms (2 seconds)
 * - Can be adjusted in application.yml or via @Value
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchmakingScheduler {

    private final MatchmakingService matchmakingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameEventProducer gameEventProducer;

    // Statistics for monitoring
    private long totalMatchesCreated = 0;
    private long totalSchedulerRuns = 0;
    private long lastRunTimestamp = 0;

    /**
     * Scheduled matchmaking task.
     *
     * Runs every 2 seconds with fixed delay (waits for previous execution to complete).
     *
     * Process:
     * 1. Check queue size
     * 2. While queue has >= 2 players:
     *    a. Call matchmakingService.findMatch()
     *    b. If match created, send WebSocket notifications
     *    c. If match failed, break loop to avoid infinite retry
     * 3. Log statistics
     */
    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    public void processMatchmakingQueue() {
        totalSchedulerRuns++;
        lastRunTimestamp = System.currentTimeMillis();

        long queueSize = matchmakingService.getQueueSize();

        if (queueSize < 2) {
            if (queueSize == 1) {
                log.debug("Matchmaking: 1 player waiting in queue");
            }
            return;
        }

        log.info("Matchmaking: Processing queue with {} players", queueSize);

        int matchesThisRun = 0;
        int maxMatchesPerRun = 10; // limit to prevent infinite loops

        // process multiple matches in one scheduler run
        while (matchmakingService.getQueueSize() >= 2 && matchesThisRun < maxMatchesPerRun) {
            try {
                GameSession session = matchmakingService.findMatch();

                if (session == null) {
                    // no match created (queue too small or error occurred)
                    break;
                }

                matchesThisRun++;
                totalMatchesCreated++;

                // send WebSocket notifications to both players, to start game
                notifyPlayersOfMatch(session);

                // publish match-created event to Kafka (async)
                publishMatchCreatedEvent(session);

                log.info("✅ Match {}/{} created in this run: game={}",
                        matchesThisRun, maxMatchesPerRun, session.getGameId());

            } catch (Exception e) {
                log.error("Error during matchmaking process: {}", e.getMessage(), e);
                //cContinue trying other matches, don't let one failure stop all matchmaking
            }
        }

        if (matchesThisRun > 0) {
            log.info("Matchmaking run complete: {} matches created, {} players remaining in queue",
                    matchesThisRun, matchmakingService.getQueueSize());
        }
    }

    /**
     * Send WebSocket notifications to matched players.
     *
     * Sends message to both players at:
     * - /user/{userId}/queue/match-found
     *
     * Message contains game ID and connection info for WebSocket game session.
     *
     * @param session Created game session
     */
    private void notifyPlayersOfMatch(GameSession session) {
        try {
            // creaing match found notification
            Map<String, Object> matchNotification = new HashMap<>();
            matchNotification.put("gameId", session.getGameId());
            matchNotification.put("gameType", "HUMAN_VS_HUMAN");
            matchNotification.put("opponentId", null); // Will be set per-player below
            matchNotification.put("websocketTopic", "/topic/game/" + session.getGameId());
            matchNotification.put("message", "Match found! Your opponent is ready.");

            // for player 1
            Map<String, Object> player1Notification = new HashMap<>(matchNotification);
            player1Notification.put("opponentId", session.getPlayer2Id());
            player1Notification.put("yourPlayerNumber", 1);
            player1Notification.put("yourColor", "BLACK");

            messagingTemplate.convertAndSendToUser(
                    session.getPlayer1Id().toString(),
                    "/queue/match-found",
                    player1Notification
            );

            // for player 2
            Map<String, Object> player2Notification = new HashMap<>(matchNotification);
            player2Notification.put("opponentId", session.getPlayer1Id());
            player2Notification.put("yourPlayerNumber", 2);
            player2Notification.put("yourColor", "WHITE");

            messagingTemplate.convertAndSendToUser(
                    session.getPlayer2Id().toString(),
                    "/queue/match-found",
                    player2Notification
            );

            log.debug("WebSocket notifications sent to players {} and {}",
                    session.getPlayer1Id(), session.getPlayer2Id());

        } catch (Exception e) {
            log.error("Failed to send match notifications for game {}: {}",
                    session.getGameId(), e.getMessage(), e);
            // don't throw exception as game was already created, notifications are best-effort
        }
    }

    /**
     * Get scheduler statistics (for monitoring/health check endpoint).
     *
     * @return Statistics map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMatchesCreated", totalMatchesCreated);
        stats.put("totalSchedulerRuns", totalSchedulerRuns);
        stats.put("lastRunTimestamp", lastRunTimestamp);
        stats.put("currentQueueSize", matchmakingService.getQueueSize());
        return stats;
    }

    /**
     * Reset statistics (for testing).
     */
    public void resetStatistics() {
        totalMatchesCreated = 0;
        totalSchedulerRuns = 0;
        lastRunTimestamp = 0;
        log.info("Matchmaking scheduler statistics reset");
    }

    /**
     * Publish match-created event to Kafka.
     *
     * Creates and publishes a MatchCreatedEvent for analytics and monitoring.
     * Async execution via GameEventProducer ensures this doesn't block matchmaking.
     *
     * @param session Created game session
     */
    private void publishMatchCreatedEvent(GameSession session) {
        try {
            MatchCreatedEvent event = MatchCreatedEvent.forMatchmaking(
                    session.getGameId(),
                    session.getPlayer1Id(),
                    session.getPlayer2Id()
            );

            gameEventProducer.publishMatchCreated(event);

        } catch (Exception e) {
            // kafka failures shouldn't break matchmaking
            log.error("Failed to publish match-created event for game {}: {}",
                    session.getGameId(), e.getMessage());
        }
    }
}
