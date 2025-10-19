package com.gomokumatching.model.dto.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID eventId;
    private UUID playerId;
    private QueueAction action;
    private int mmr; // for future mmr-based matching

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime timestamp;

    public enum QueueAction {
        PLAYER_JOINED,
        PLAYER_LEFT,
        PLAYER_TIMEOUT  // future feature
    }

    public static QueueEvent playerJoined(UUID playerId) {
        return QueueEvent.builder()
                .eventId(UUID.randomUUID())
                .playerId(playerId)
                .action(QueueAction.PLAYER_JOINED)
                .mmr(0)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static QueueEvent playerLeft(UUID playerId) {
        return QueueEvent.builder()
                .eventId(UUID.randomUUID())
                .playerId(playerId)
                .action(QueueAction.PLAYER_LEFT)
                .mmr(0)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static QueueEvent playerTimeout(UUID playerId) {
        return QueueEvent.builder()
                .eventId(UUID.randomUUID())
                .playerId(playerId)
                .action(QueueAction.PLAYER_TIMEOUT)
                .mmr(0)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
