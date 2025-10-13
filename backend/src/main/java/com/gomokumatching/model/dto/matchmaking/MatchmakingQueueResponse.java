package com.gomokumatching.model.dto.matchmaking;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for matchmaking queue operations.
 *
 * Provides queue status information to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchmakingQueueResponse {

    /**
     * Status of the matchmaking request
     */
    private QueueStatus status;

    /**
     * Message describing the current state
     */
    private String message;

    /**
     * Player's position in queue (1 = first in line, null if not in queue)
     */
    private Integer queuePosition;

    /**
     * Total number of players in queue
     */
    private Long totalPlayersInQueue;

    /**
     * Estimated wait time in seconds (null if cannot estimate)
     */
    private Integer estimatedWaitSeconds;

    /**
     * Timestamp when player joined queue
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinedAt;

    /**
     * Queue status enum
     */
    public enum QueueStatus {
        JOINED,
        ALREADY_IN_QUEUE,
        LEFT,
        NOT_IN_QUEUE,
        MATCH_FOUND,
        ERROR
    }

    /**
     * Create response for successful queue join
     */
    public static MatchmakingQueueResponse joined(int position, long total, LocalDateTime joinedAt) {
        return MatchmakingQueueResponse.builder()
                .status(QueueStatus.JOINED)
                .message("Successfully joined matchmaking queue")
                .queuePosition(position)
                .totalPlayersInQueue(total)
                .estimatedWaitSeconds(estimateWaitTime(position))
                .joinedAt(joinedAt)
                .build();
    }

    /**
     * Create response for already in queue
     */
    public static MatchmakingQueueResponse alreadyInQueue(int position, long total, LocalDateTime joinedAt) {
        return MatchmakingQueueResponse.builder()
                .status(QueueStatus.ALREADY_IN_QUEUE)
                .message("You are already in the matchmaking queue")
                .queuePosition(position)
                .totalPlayersInQueue(total)
                .estimatedWaitSeconds(estimateWaitTime(position))
                .joinedAt(joinedAt)
                .build();
    }

    /**
     * Create response for successful queue leave
     */
    public static MatchmakingQueueResponse left() {
        return MatchmakingQueueResponse.builder()
                .status(QueueStatus.LEFT)
                .message("Successfully left matchmaking queue")
                .queuePosition(null)
                .totalPlayersInQueue(null)
                .estimatedWaitSeconds(null)
                .joinedAt(null)
                .build();
    }

    /**
     * Create response for not in queue
     */
    public static MatchmakingQueueResponse notInQueue() {
        return MatchmakingQueueResponse.builder()
                .status(QueueStatus.NOT_IN_QUEUE)
                .message("You are not in the matchmaking queue")
                .queuePosition(null)
                .totalPlayersInQueue(null)
                .estimatedWaitSeconds(null)
                .joinedAt(null)
                .build();
    }

    /**
     * Estimate wait time based on queue position
     * Simple heuristic: assume 5 seconds per match ahead in queue
     */
    private static Integer estimateWaitTime(int position) {
        if (position <= 2) {
            return 5;
        }
        // every 2 players = 1 match, each match takes ~5 seconds to form
        // TODO: make estimated wait time dynamic based on analytics/# of online players
        int matchesAhead = (position - 1) / 2;
        return matchesAhead * 5;
    }
}
