package com.gomokumatching.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Professional JWT Token Provider with industry best practices.
 *
 * Security Features:
 * - HMAC-SHA512 algorithm for strong signing
 * - Configurable expiration times
 * - Secure secret key from environment
 * - Comprehensive validation
 * - Proper exception handling
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:900000}") // Default 15 minutes
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // Default 7 days
    private long jwtRefreshExpirationMs;

    private SecretKey key;

    /**
     * Initialize the signing key after properties are injected.
     * Uses HMAC-SHA512 for maximum security.
     */
    @PostConstruct
    public void init() {
        // Generate secure key from secret string
        // For production: use a properly generated 512-bit secret
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT token from authenticated user.
     *
     * @param authentication Spring Security authentication object
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return generateTokenFromUserId(userPrincipal.getId());
    }

    /**
     * Generate JWT token from user ID.
     *
     * @param userId User's UUID
     * @return JWT token string
     */
    public String generateTokenFromUserId(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Generate refresh token with longer expiration.
     *
     * @param userId User's UUID
     * @return Refresh token string
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token JWT token string
     * @return User ID as UUID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return UUID.fromString(claims.getSubject());
    }

    /**
     * Validate JWT token.
     *
     * @param token JWT token string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Get expiration time in milliseconds.
     *
     * @return Expiration time
     */
    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    /**
     * Get expiration time in seconds (for response).
     *
     * @return Expiration time in seconds
     */
    public long getExpirationInSeconds() {
        return jwtExpirationMs / 1000;
    }
}
