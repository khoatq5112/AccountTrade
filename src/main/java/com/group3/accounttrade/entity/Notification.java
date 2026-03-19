package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing user notifications for order updates, 
 * payment status, escrow changes, dispute updates, etc.
 */
@Data
@Entity
@Table(name = "notifications")
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
     * Values: ORDER, PAYMENT, ESCROW, CREDENTIAL, DISPUTE, SYSTEM, PROMO
     */
    @Column(name = "notification_type", length = 50, nullable = false)
    private String notificationType;

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
     * Priority level (1=low, 2=normal, 3=high, 4=urgent).
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 2;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Notification type constants
    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_ESCROW = "ESCROW";
    public static final String TYPE_CREDENTIAL = "CREDENTIAL";
    public static final String TYPE_DISPUTE = "DISPUTE";
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_PROMO = "PROMO";

    // Priority constants
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_URGENT = 4;
}
