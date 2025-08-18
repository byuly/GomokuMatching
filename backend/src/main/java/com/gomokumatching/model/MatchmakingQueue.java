package com.gomokumatching.model;

import com.gomokumatching.model.enums.DifficultyLevelEnum;
import com.gomokumatching.model.enums.PreferredOpponentEnum;
import com.gomokumatching.model.enums.QueueStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "matchmaking_queue", schema = "gomoku")
public class MatchmakingQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "queue_id")
    private UUID queueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_opponent", columnDefinition = "preferred_opponent_enum")
    private PreferredOpponentEnum preferredOpponent = PreferredOpponentEnum.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_difficulty", columnDefinition = "difficulty_level_enum")
    private DifficultyLevelEnum aiDifficulty;

    @Column(name = "mmr_min")
    private Integer mmrMin;

    @Column(name = "mmr_max")
    private Integer mmrMax;

    @CreationTimestamp
    @Column(name = "queued_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime queuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_status", columnDefinition = "queue_status_enum")
    private QueueStatusEnum queueStatus = QueueStatusEnum.WAITING;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime expiresAt;
}
