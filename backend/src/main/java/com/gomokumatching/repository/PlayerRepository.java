package com.gomokumatching.repository;

import com.gomokumatching.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Player entity.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {
}
