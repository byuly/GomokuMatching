package com.gomokumatching.model;

import com.gomokumatching.model.enums.AccountStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "player", schema = "gomoku")
public class Player {

    @Id
    @Column(name = "player_id")
    private String playerId; // Changed to String to store Firebase UID

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @Column(name = "last_login", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime lastLogin;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", columnDefinition = "account_status_enum")
    private AccountStatusEnum accountStatus = AccountStatusEnum.ACTIVE;

    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PlayerStats stats;

    @OneToMany(mappedBy = "player")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MatchmakingQueue> matchmakingQueues;

    @OneToMany(mappedBy = "player")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<GameSession> gameSessions;
}