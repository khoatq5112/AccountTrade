package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing user notification preferences.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * Find notification preferences by user ID.
     */
    Optional<NotificationPreference> findByUser_UserId(Integer userId);

    /**
     * Check if notification preferences exist for a user.
     */
    boolean existsByUser_UserId(Integer userId);
}
