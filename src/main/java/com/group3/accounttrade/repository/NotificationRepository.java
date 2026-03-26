package com.group3.accounttrade.repository;

import com.group3.accounttrade.entity.Notification;
import com.group3.accounttrade.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Notification entity.
 * Manages user notifications for order updates, disputes, etc.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser(User user);

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUser(@Param("user") User user);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    long countUnreadByUser(@Param("user") User user);

    List<Notification> findByUserAndNotificationType(User user, String notificationType);

    Page<Notification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Page<Notification> findByUser_UserIdAndNotificationTypeOrderByCreatedAtDesc(
            Integer userId, String notificationType, Pageable pageable);

    Page<Notification> findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Page<Notification> findByUser_UserIdAndNotificationTypeAndIsReadFalseOrderByCreatedAtDesc(
            Integer userId, String notificationType, Pageable pageable);

    List<Notification> findByUser_UserIdAndIsReadFalse(Integer userId);

    List<Notification> findByUser_UserIdAndNotificationTypeAndIsReadFalse(Integer userId, String notificationType);

    long countByUser_UserIdAndIsReadFalse(Integer userId);

    @Query("""
            SELECT n.notificationType, COUNT(n)
            FROM Notification n
            WHERE n.user.userId = :userId AND n.isRead = false
            GROUP BY n.notificationType
            """)
    List<Object[]> countUnreadByTypeAndUserId(@Param("userId") Integer userId);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.notificationType = :notificationType AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserAndNotificationType(
            @Param("user") User user,
            @Param("notificationType") String notificationType);

    @Query(value = "SELECT * FROM notifications n WHERE n.user_id = :userId AND n.is_read = false ORDER BY n.created_at DESC LIMIT :limit", nativeQuery = true)
    List<Notification> findRecentUnreadByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadForUser(@Param("user") User user);

    @Query("SELECT n FROM Notification n WHERE n.relatedEntityType = :entityType AND n.relatedEntityId = :entityId ORDER BY n.createdAt DESC")
    List<Notification> findByRelatedEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.emailSent = false AND n.emailSentAt IS NULL")
    List<Notification> findPendingEmailNotifications(@Param("user") User user);

    @Query("SELECT n FROM Notification n WHERE n.emailSent = false AND n.createdAt < :cutoffTime")
    List<Notification> findUnsentEmailNotificationsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("""
            SELECT n.notificationType, COUNT(n)
            FROM Notification n
            GROUP BY n.notificationType
            """)
    List<Object[]> countByType();

    long countByEmailSentFalse();
}
