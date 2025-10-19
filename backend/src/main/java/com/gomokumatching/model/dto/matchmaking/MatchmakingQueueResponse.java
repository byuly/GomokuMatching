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

    private QueueStatus status;
    private String message;
    private Integer queuePosition;
    private Long totalPlayersInQueue;
    private Integer estimatedWaitSeconds;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinedAt;

    public enum QueueStatus {
        JOINED,
        ALREADY_IN_QUEUE,
        LEFT,
        NOT_IN_QUEUE,
        MATCH_FOUND,
        ERROR
    }

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

    // simple heuristic: 5 seconds per match ahead in queue
    private static Integer estimateWaitTime(int position) {
        if (position <= 2) {
            return 5;
        }
        int matchesAhead = (position - 1) / 2;
        return matchesAhead * 5;
    }
}
