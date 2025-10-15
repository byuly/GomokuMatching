package com.gomokumatching.model;

import com.gomokumatching.model.enums.GameStatusEnum;
import com.gomokumatching.model.enums.GameTypeEnum;
import com.gomokumatching.model.enums.WinnerTypeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "game", schema = "gomoku")
public class Game {

    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameTypeEnum gameType;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_status")
    private GameStatusEnum gameStatus = GameStatusEnum.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player2;

    @Column(name = "ai_difficulty", length = 20)
    private String aiDifficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_type")
    private WinnerTypeEnum winnerType = WinnerTypeEnum.NONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player winner;

    @Column(name = "total_moves")
    private int totalMoves = 0;

    @Column(name = "started_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_board_state", columnDefinition = "jsonb")
    private String finalBoardState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "move_sequence", columnDefinition = "jsonb")
    private String moveSequence;

    @Column(name = "game_duration_seconds")
    private Integer gameDurationSeconds;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<GameMove> moves;
}
