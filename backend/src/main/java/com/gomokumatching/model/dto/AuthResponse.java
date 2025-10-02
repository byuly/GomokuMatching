package com.gomokumatching.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for authentication response (login/register).
 *
 * Contains JWT tokens and user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UUID userId;
    private String username;
    private String email;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // seconds until access token expires

    /**
     * Create response with access token only (no refresh token).
     */
    public static AuthResponse withAccessToken(
            UUID userId,
            String username,
            String email,
            String accessToken,
            Long expiresIn
    ) {
        return AuthResponse.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }

    /**
     * Create response with both access and refresh tokens.
     */
    public static AuthResponse withRefreshToken(
            UUID userId,
            String username,
            String email,
            String accessToken,
            String refreshToken,
            Long expiresIn
    ) {
        return AuthResponse.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
