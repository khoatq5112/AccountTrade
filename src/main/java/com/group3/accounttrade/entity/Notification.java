package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing user notifications for order updates,
 * payment status, escrow changes, dispute updates, etc.
 */
@Data
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_unread", columnList = "user_id, is_read, created_at"),
    @Index(name = "idx_user_type", columnList = "user_id, notification_type"),
    @Index(name = "idx_entity", columnList = "related_entity_type, related_entity_id"),
    @Index(name = "idx_email_pending", columnList = "email_sent, priority, created_at")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Type of notification.
     * Values: ORDER, PAYMENT, ESCROW, CREDENTIAL, DISPUTE, SYSTEM, PROMO, WALLET, POST, REVIEW
     */
    @Column(name = "notification_type", length = 50, nullable = false)
    private String notificationType;

    /**
     * Category for grouping notifications.
     * Values: ORDER, PAYMENT, ESCROW, CREDENTIAL, DISPUTE, SYSTEM, PROMO, WALLET, POST, REVIEW
     */
    @Column(name = "notification_category", length = 50)
    private String notificationCategory;

    /**
     * Notification title.
     */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /**
     * Notification message content.
     */
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * Template code used to generate this notification.
     */
    @Column(name = "template_code", length = 100)
    private String templateCode;

    /**
     * Variables used for template substitution (JSON format).
     */
    @Column(name = "template_variables", columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> templateVariables;

    /**
     * Related entity type (e.g., Order, Payment, Dispute).
     */
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /**
     * ID of the related entity.
     */
    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    /**
     * URL to navigate when notification is clicked.
     */
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    /**
     * Priority level (1=low, 2=normal, 3=high, 4=urgent).
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 2;

    /**
     * Whether the notification has been read.
     */
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    /**
     * Timestamp when the notification was read.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Whether the notification has been archived.
     */
    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    /**
     * Timestamp when the notification was archived.
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /**
     * Whether the notification was delivered via in-app.
     */
    @Column(name = "in_app_sent")
    @Builder.Default
    private Boolean inAppSent = true;

    /**
     * Timestamp when in-app notification was sent.
     */
    @Column(name = "in_app_sent_at")
    private LocalDateTime inAppSentAt;

    /**
     * Whether the notification has been sent via email.
     */
    @Column(name = "email_sent")
    @Builder.Default
    private Boolean emailSent = false;

    /**
     * Timestamp when email was sent.
     */
    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    /**
     * Error message if email delivery failed.
     */
    @Column(name = "email_error", columnDefinition = "TEXT")
    private String emailError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * When this notification expires and should be cleaned up.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Notification type constants
    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_ESCROW = "ESCROW";
    public static final String TYPE_CREDENTIAL = "CREDENTIAL";
    public static final String TYPE_DISPUTE = "DISPUTE";
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_PROMO = "PROMO";
    public static final String TYPE_WALLET = "WALLET";
    public static final String TYPE_POST = "POST";
    public static final String TYPE_REVIEW = "REVIEW";
    public static final String TYPE_USER = "USER";

    // Priority constants
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_URGENT = 4;
}
