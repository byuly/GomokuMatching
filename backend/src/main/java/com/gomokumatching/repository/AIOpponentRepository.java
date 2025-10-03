package com.gomokumatching.repository;

import com.gomokumatching.model.AIOpponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the AIOpponent entity.
 */
@Repository
public interface AIOpponentRepository extends JpaRepository<AIOpponent, UUID> {

    /**
     * Find AI opponents by difficulty level
     */
    List<AIOpponent> findByDifficultyLevel(String difficultyLevel);

    /**
     * Find active AI opponents by difficulty
     */
    @Query("SELECT ai FROM AIOpponent ai WHERE ai.difficultyLevel = :difficultyLevel AND ai.isActive = true")
    Optional<AIOpponent> findActiveByDifficultyLevel(String difficultyLevel);

    /**
     * Find all active AI opponents
     */
    List<AIOpponent> findByIsActiveTrue();

    /**
     * Find AI opponent by name
     */
    Optional<AIOpponent> findByName(String name);
}
