package com.gomokumatching.controller;

import com.gomokumatching.model.dto.matchmaking.MatchmakingQueueResponse;
import com.gomokumatching.security.CustomUserDetails;
import com.gomokumatching.service.MatchmakingScheduler;
import com.gomokumatching.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for matchmaking operations.
 *
 * Handles:
 * - Joining matchmaking queue
 * - Leaving matchmaking queue
 * - Getting queue status
 * - Admin/monitoring endpoints
 *
 * Flow:
 * 1. Player calls POST /api/matchmaking/queue to join
 * 2. MatchmakingScheduler automatically creates match when >= 2 players
 * 3. Players receive WebSocket notification at /user/{userId}/queue/match-found
 * 4. Players connect to game via WebSocket at /topic/game/{gameId}
 */
@RestController
@RequestMapping("/api/matchmaking")
@RequiredArgsConstructor
@Slf4j
public class MatchmakingController {

    private final MatchmakingService matchmakingService;
    private final MatchmakingScheduler matchmakingScheduler;

    /**
     * Join matchmaking queue.
     *
     * POST /api/matchmaking/queue
     *
     * If player is already in queue, returns current queue status.
     * Player will receive WebSocket notification when match is found.
     *
     * @param currentUser Authenticated user
     * @return 200 OK with queue status (position, estimated wait time)
     */
    @PostMapping("/queue")
    public ResponseEntity<MatchmakingQueueResponse> joinQueue(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();

        log.info("Player {} requesting to join matchmaking queue", playerId);

        MatchmakingQueueResponse response = matchmakingService.joinQueue(playerId);

        // return 200 for both JOINED and ALREADY_IN_QUEUE
        // client should check response status to determine if they newly joined
        return ResponseEntity.ok(response);
    }

    /**
     * Leave matchmaking queue.
     *
     * DELETE /api/matchmaking/queue
     *
     * Removes player from queue. If player is not in queue, returns 200 with NOT_IN_QUEUE status.
     *
     * @param currentUser Authenticated user
     * @return 200 OK with confirmation
     */
    @DeleteMapping("/queue")
    public ResponseEntity<MatchmakingQueueResponse> leaveQueue(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();

        log.info("Player {} requesting to leave matchmaking queue", playerId);

        MatchmakingQueueResponse response = matchmakingService.leaveQueue(playerId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current queue status.
     *
     * GET /api/matchmaking/status
     *
     * Returns player's position in queue and estimated wait time.
     * If not in queue, returns NOT_IN_QUEUE status.
     *
     * @param currentUser Authenticated user
     * @return 200 OK with queue status
     */
    @GetMapping("/status")
    public ResponseEntity<MatchmakingQueueResponse> getQueueStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();

        log.debug("Player {} requesting queue status", playerId);

        MatchmakingQueueResponse response = matchmakingService.getQueueStatus(playerId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get matchmaking statistics (admin/monitoring endpoint).
     *
     * GET /api/matchmaking/stats
     *
     * Returns:
     * - Total matches created
     * - Current queue size
     * - Scheduler statistics
     *
     * Note: In production, this should be restricted to admin role only.
     *
     * @return 200 OK with statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.debug("Matchmaking statistics requested");

        Map<String, Object> stats = matchmakingScheduler.getStatistics();

        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint for matchmaking system.
     *
     * GET /api/matchmaking/health
     *
     * Returns basic health information about matchmaking.
     *
     * @return 200 OK if healthy
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        long queueSize = matchmakingService.getQueueSize();
        Map<String, Object> stats = matchmakingScheduler.getStatistics();

        Map<String, Object> health = Map.of(
                "status", "UP",
                "currentQueueSize", queueSize,
                "totalMatchesCreated", stats.get("totalMatchesCreated"),
                "lastSchedulerRun", stats.get("lastRunTimestamp")
        );

        return ResponseEntity.ok(health);
    }
}
