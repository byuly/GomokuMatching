package com.gomokumatching.model;

import com.gomokumatching.model.enums.PlayerTypeEnum;
import com.gomokumatching.model.enums.StoneColorEnum;
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
@Table(name = "game_move", schema = "gomoku",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"game_id", "move_number"}),
                @UniqueConstraint(columnNames = {"game_id", "board_x", "board_y"})
        })
public class GameMove {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "move_id")
    private UUID moveId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Game game;

    @Column(name = "move_number", nullable = false)
    private int moveNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "player_type", nullable = false)
    private PlayerTypeEnum playerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_opponent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AIOpponent aiOpponent;

    @Column(name = "board_x", nullable = false)
    private int boardX;

    @Column(name = "board_y", nullable = false)
    private int boardY;

    @Enumerated(EnumType.STRING)
    @Column(name = "stone_color", nullable = false)
    private StoneColorEnum stoneColor;

    @CreationTimestamp
    @Column(name = "move_timestamp", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime moveTimestamp;

    @Column(name = "time_taken_ms")
    private Integer timeTakenMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "board_state_after_move", columnDefinition = "jsonb")
    private String boardStateAfterMove;
}
