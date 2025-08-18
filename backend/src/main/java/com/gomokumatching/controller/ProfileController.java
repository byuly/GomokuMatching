package com.gomokumatching.controller;

import com.gomokumatching.service.PlayerService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling user profile operations.
 */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final PlayerService playerService;

    /**
     * Endpoint for syncing a Firebase user with a local database profile.
     *
     * How it works:
     * 1. This endpoint should be called by the frontend once, immediately after a user's first successful login.
     * 2. The request must include a valid Firebase ID token, which is verified by the FirebaseFilter.
     * 3. The `Authentication` object, injected by Spring Security, contains the verified FirebaseToken.
     * 4. The `playerService` uses the token to find an existing player or create a new one.
     *
     * @param authentication The authenticated user principal, containing the FirebaseToken.
     * @return A response indicating success or failure.
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncUserProfile(Authentication authentication) {
        FirebaseToken decodedToken = (FirebaseToken) authentication.getCredentials();
        playerService.syncPlayer(decodedToken);
        return ResponseEntity.ok("Profile synced for user: " + decodedToken.getUid());
    }
}
