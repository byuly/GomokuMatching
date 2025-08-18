package com.gomokumatching.service;

import com.gomokumatching.model.Player;
import com.gomokumatching.repository.PlayerRepository;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    @Override
    @Transactional
    public Player syncPlayer(FirebaseToken decodedToken) {
        // Find player by Firebase UID. If present, return it.
        Optional<Player> playerOptional = playerRepository.findById(decodedToken.getUid());
        if (playerOptional.isPresent()) {
            return playerOptional.get();
        }

        // Otherwise, create a new player profile.
        Player newPlayer = new Player();
        newPlayer.setPlayerId(decodedToken.getUid());
        newPlayer.setEmail(decodedToken.getEmail());
        
        // Use display name for username, fallback to email if not available
        String username = decodedToken.getName() != null && !decodedToken.getName().isEmpty() 
                            ? decodedToken.getName() 
                            : decodedToken.getEmail();
        newPlayer.setUsername(username);

        return playerRepository.save(newPlayer);
    }
}
