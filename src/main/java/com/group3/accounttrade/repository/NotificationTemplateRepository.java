package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing notification templates.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * Find notification template by template code.
     */
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);

    Optional<NotificationTemplate> findByTemplateCodeAndIsActiveTrue(String templateCode);

    /**
     * Find all templates by notification type.
     */
    java.util.List<NotificationTemplate> findByNotificationType(String notificationType);

    /**
     * Find all active templates.
     */
    java.util.List<NotificationTemplate> findByIsActiveTrue();

    /**
     * Find template by type and category.
     */
    Optional<NotificationTemplate> findByNotificationTypeAndNotificationCategory(
            String notificationType, String notificationCategory);

    /**
     * Check if template code exists.
     */
    boolean existsByTemplateCode(String templateCode);
}
