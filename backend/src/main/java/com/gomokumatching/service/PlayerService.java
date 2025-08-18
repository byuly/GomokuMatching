package com.gomokumatching.service;

import com.gomokumatching.model.Player;
import com.google.firebase.auth.FirebaseToken;

/**
 * Service interface for player-related operations.
 */
public interface PlayerService {
    /**
     * Syncs a Firebase user with the local database. If the user doesn't exist, it creates a new profile.
     * @param decodedToken The verified Firebase ID token.
     * @return The existing or newly created Player profile.
     */
    Player syncPlayer(FirebaseToken decodedToken);
}
