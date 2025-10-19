package com.gomokumatching.model.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.*;

// matchmaking queue state for kafka streams aggregation
// immutable updates, stored in rocksdb-backed state store
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchmakingState implements Serializable {

    private static final long serialVersionUID = 1L;

    // linkedhashmap preserves FIFO insertion order
    @Builder.Default
    private Map<UUID, OffsetDateTime> waitingPlayers = new LinkedHashMap<>();

    // deduplication: prevents matching same player twice during async PLAYER_LEFT event processing
    @Builder.Default
    private Set<UUID> matchedPlayers = new HashSet<>();

    @Builder.Default
    private long totalJoined = 0;

    @Builder.Default
    private long totalMatchesCreated = 0;

    public MatchmakingState addPlayer(QueueEvent event) {
        Map<UUID, OffsetDateTime> newWaitingPlayers = new LinkedHashMap<>(this.waitingPlayers);
        Set<UUID> newMatchedPlayers = new HashSet<>(this.matchedPlayers);

        if (!newWaitingPlayers.containsKey(event.getPlayerId()) &&
                !newMatchedPlayers.contains(event.getPlayerId())) {
            newWaitingPlayers.put(event.getPlayerId(), event.getTimestamp());
        }

        return MatchmakingState.builder()
                .waitingPlayers(newWaitingPlayers)
                .matchedPlayers(newMatchedPlayers)
                .totalJoined(this.totalJoined + 1)
                .totalMatchesCreated(this.totalMatchesCreated)
                .build();
    }

    public MatchmakingState removePlayer(QueueEvent event) {
        Map<UUID, OffsetDateTime> newWaitingPlayers = new LinkedHashMap<>(this.waitingPlayers);
        Set<UUID> newMatchedPlayers = new HashSet<>(this.matchedPlayers);

        newWaitingPlayers.remove(event.getPlayerId());
        newMatchedPlayers.remove(event.getPlayerId());

        return MatchmakingState.builder()
                .waitingPlayers(newWaitingPlayers)
                .matchedPlayers(newMatchedPlayers)
                .totalJoined(this.totalJoined)
                .totalMatchesCreated(this.totalMatchesCreated)
                .build();
    }

    public int getWaitingCount() {
        return waitingPlayers.size();
    }

    public boolean canCreateMatch() {
        long availablePlayers = waitingPlayers.keySet().stream()
                .filter(playerId -> !matchedPlayers.contains(playerId))
                .count();
        return availablePlayers >= 2;
    }

    // pop 2 oldest players (FIFO), mark as matched, return new state
    // note: players stay in waitingPlayers until PLAYER_LEFT events processed
    public MatchResult createMatch() {
        if (!canCreateMatch()) {
            return MatchResult.noMatch(this);
        }

        Iterator<UUID> iterator = waitingPlayers.keySet().stream()
                .filter(playerId -> !matchedPlayers.contains(playerId))
                .iterator();

        if (!iterator.hasNext()) {
            return MatchResult.noMatch(this);
        }
        UUID player1Id = iterator.next();

        if (!iterator.hasNext()) {
            return MatchResult.noMatch(this);
        }
        UUID player2Id = iterator.next();

        Set<UUID> newMatchedPlayers = new HashSet<>(this.matchedPlayers);
        newMatchedPlayers.add(player1Id);
        newMatchedPlayers.add(player2Id);

        MatchmakingState newState = MatchmakingState.builder()
                .waitingPlayers(new LinkedHashMap<>(this.waitingPlayers))
                .matchedPlayers(newMatchedPlayers)
                .totalJoined(this.totalJoined)
                .totalMatchesCreated(this.totalMatchesCreated + 1)
                .build();

        return MatchResult.matchCreated(player1Id, player2Id, newState);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private boolean matchCreated;
        private UUID player1Id;
        private UUID player2Id;
        private MatchmakingState newState;

        public static MatchResult matchCreated(UUID player1Id, UUID player2Id, MatchmakingState newState) {
            return MatchResult.builder()
                    .matchCreated(true)
                    .player1Id(player1Id)
                    .player2Id(player2Id)
                    .newState(newState)
                    .build();
        }

        public static MatchResult noMatch(MatchmakingState currentState) {
            return MatchResult.builder()
                    .matchCreated(false)
                    .player1Id(null)
                    .player2Id(null)
                    .newState(currentState)
                    .build();
        }
    }
}
