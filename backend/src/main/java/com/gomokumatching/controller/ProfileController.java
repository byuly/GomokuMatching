package com.gomokumatching.controller;

import com.gomokumatching.model.dto.PlayerProfileDTO;
import com.gomokumatching.model.dto.UpdateUsernameRequest;
import com.gomokumatching.security.CustomUserDetails;
import com.gomokumatching.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for user profile operations.
 *
 * Endpoints:
 * - GET /api/profiles/me - Get current user's profile
 * - PUT /api/profiles/username - Update username
 *
 * All endpoints require authentication (JWT token).
 */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final PlayerService playerService;

    /**
     * Get authenticated user's profile.
     *
     * @param authentication Spring Security authentication object
     * @return User profile DTO
     */
    @GetMapping("/me")
    public ResponseEntity<PlayerProfileDTO> getCurrentUserProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getId();

        logger.debug("Fetching profile for user ID: {}", userId);

        PlayerProfileDTO profile = playerService.getPlayerProfile(userId);

        return ResponseEntity.ok(profile);
    }

    /**
     * Update authenticated user's username.
     *
     * @param authentication Spring Security authentication object
     * @param request Request containing new username
     * @return Success message
     */
    @PutMapping("/username")
    public ResponseEntity<String> updateUsername(
            Authentication authentication,
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getId();

        logger.info("Username update request for user ID: {}", userId);

        playerService.updateUsername(userId, request.getUsername());

        logger.info("Username updated successfully for user ID: {}", userId);

        return ResponseEntity.ok("Username updated successfully");
    }
}
