package com.gomokumatching.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "game_session", schema = "gomoku")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "session_id")
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player;

    @Column(name = "websocket_session_id", nullable = false)
    private String websocketSessionId;

    @CreationTimestamp
    @Column(name = "connected_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime connectedAt;

    @Column(name = "disconnected_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime disconnectedAt;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "reconnect_count")
    private int reconnectCount = 0;
}
