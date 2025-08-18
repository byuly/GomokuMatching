package com.gomokumatching.model;

import com.gomokumatching.model.enums.GameOutcomeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "game_analytics", schema = "gomoku")
public class GameAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "analytics_id")
    private UUID analyticsId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_outcome", nullable = false, columnDefinition = "game_outcome_enum")
    private GameOutcomeEnum gameOutcome;

    @Column(name = "game_length_moves")
    private Integer gameLengthMoves;

    @Column(name = "average_move_time_ms")
    private Integer averageMoveTimeMs;

    @Column(name = "player1_total_time_ms")
    private Integer player1TotalTimeMs;

    @Column(name = "player2_total_time_ms")
    private Integer player2TotalTimeMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_moves_pattern", columnDefinition = "jsonb")
    private String openingMovesPattern;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "winning_pattern", columnDefinition = "jsonb")
    private String winningPattern;

    @Column(name = "had_disconnections")
    private boolean hadDisconnections = false;

    @CreationTimestamp
    @Column(name = "analyzed_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime analyzedAt;
}
