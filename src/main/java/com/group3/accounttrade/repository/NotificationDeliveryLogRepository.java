package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.NotificationDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing notification delivery logs.
 */
@Repository
public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, Long> {

    /**
     * Find all delivery logs for a notification.
     */
    List<NotificationDeliveryLog> findByNotificationNotificationId(Long notificationId);

    /**
     * Find delivery logs by notification and channel.
     */
    List<NotificationDeliveryLog> findByNotificationNotificationIdAndChannel(
            Long notificationId, String channel);

    /**
     * Find pending delivery attempts that need retry.
     */
    List<NotificationDeliveryLog> findByStatusAndNextRetryAtBefore(
            String status, LocalDateTime nextRetryAtBefore);

    /**
     * Find delivery logs by status.
     */
    List<NotificationDeliveryLog> findByStatus(String status);

    /**
     * Find failed delivery attempts.
     */
    List<NotificationDeliveryLog> findByStatusOrderByAttemptedAtDesc(String status);

    /**
     * Count delivery logs by status.
     */
    long countByStatus(String status);

    /**
     * Find delivery logs for a notification ordered by attempt time.
     */
    List<NotificationDeliveryLog> findByNotificationNotificationIdOrderByAttemptedAtDesc(Long notificationId);

    @Query("""
            SELECT l.channel, COUNT(l)
            FROM NotificationDeliveryLog l
            GROUP BY l.channel
            """)
    List<Object[]> countByChannel();
}
