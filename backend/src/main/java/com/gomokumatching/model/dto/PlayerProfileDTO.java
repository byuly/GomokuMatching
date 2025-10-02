package com.gomokumatching.model.dto;

import com.gomokumatching.model.enums.AccountStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for player profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProfileDTO {
    private UUID playerId;
    private String username;
    private String email;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLogin;
    private AccountStatusEnum accountStatus;
}
