package com.gomokumatching.controller;

import com.gomokumatching.model.dto.matchmaking.MatchmakingQueueResponse;
import com.gomokumatching.security.CustomUserDetails;
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

    @PostMapping("/queue")
    public ResponseEntity<MatchmakingQueueResponse> joinQueue(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();
        log.info("Player {} requesting to join matchmaking queue", playerId);

        MatchmakingQueueResponse response = matchmakingService.joinQueue(playerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/queue")
    public ResponseEntity<MatchmakingQueueResponse> leaveQueue(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();
        log.info("Player {} requesting to leave matchmaking queue", playerId);

        MatchmakingQueueResponse response = matchmakingService.leaveQueue(playerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<MatchmakingQueueResponse> getQueueStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID playerId = currentUser.getId();
        log.debug("Player {} requesting queue status", playerId);

        MatchmakingQueueResponse response = matchmakingService.getQueueStatus(playerId);
        return ResponseEntity.ok(response);
    }

    // todo: restrict to admin role after adding role-based access
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(Map.of(
                "architecture", "event-driven-kafka-streams",
                "message", "detailed stats available via kafka metrics"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "architecture", "kafka-streams"
        ));
    }
}
