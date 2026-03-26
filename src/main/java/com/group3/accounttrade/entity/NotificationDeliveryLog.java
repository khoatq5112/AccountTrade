package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing notification delivery log for auditing and retry.
 * Tracks all delivery attempts across channels (IN_APP, EMAIL).
 */
@Data
@Entity
@Table(name = "notification_delivery_log")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    /**
     * Delivery channel: IN_APP, EMAIL.
     */
    @Column(name = "channel", length = 20, nullable = false)
    private String channel;

    /**
     * Delivery status: PENDING, SENT, DELIVERED, FAILED, RETRY.
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /**
     * Number of delivery attempts.
     */
    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 1;

    /**
     * Response from the email service provider.
     */
    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    /**
     * Error message if delivery failed.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp when delivery was attempted.
     */
    @CreationTimestamp
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt;

    /**
     * Timestamp when notification was successfully delivered.
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * When to retry if delivery failed.
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    // Channel constants
    public static final String CHANNEL_IN_APP = "IN_APP";
    public static final String CHANNEL_EMAIL = "EMAIL";

    // Status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RETRY = "RETRY";
}
