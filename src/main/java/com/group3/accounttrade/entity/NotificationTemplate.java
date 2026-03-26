package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing notification templates for consistent messaging.
 * Stores title, message, and email templates with variable placeholders.
 */
@Data
@Entity
@Table(name = "notification_templates")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    /**
     * Unique code identifying this template (e.g., ORDER_CREATED, PAYMENT_RECEIVED).
     */
    @Column(name = "template_code", length = 100, nullable = false, unique = true)
    private String templateCode;

    /**
     * Type of notification this template is for.
     */
    @Column(name = "notification_type", length = 50, nullable = false)
    private String notificationType;

    /**
     * Category for grouping (e.g., ORDER, PAYMENT, DISPUTE).
     */
    @Column(name = "notification_category", length = 50)
    private String notificationCategory;

    /**
     * Title template with variable placeholders (e.g., "Order #{orderNumber} Confirmed").
     */
    @Column(name = "title_template", length = 200, nullable = false)
    private String titleTemplate;

    /**
     * Message template with variable placeholders.
     */
    @Column(name = "message_template", columnDefinition = "TEXT", nullable = false)
    private String messageTemplate;

    /**
     * Email subject template.
     */
    @Column(name = "email_subject_template", length = 200)
    private String emailSubjectTemplate;

    /**
     * Email body template (HTML).
     */
    @Column(name = "email_body_template", columnDefinition = "TEXT")
    private String emailBodyTemplate;

    /**
     * Default priority for notifications using this template.
     */
    @Column(name = "default_priority")
    @Builder.Default
    private Integer defaultPriority = 2;

    /**
     * Default action URL template with placeholders.
     */
    @Column(name = "default_action_url_template", length = 500)
    private String defaultActionUrlTemplate;

    /**
     * Whether this template is active.
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Template code constants
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";
    public static final String PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    public static final String ESCROW_CREATED = "ESCROW_CREATED";
    public static final String ESCROW_RELEASED = "ESCROW_RELEASED";
    public static final String ESCROW_REFUNDED = "ESCROW_REFUNDED";
    public static final String DISPUTE_OPENED = "DISPUTE_OPENED";
    public static final String DISPUTE_RESOLVED = "DISPUTE_RESOLVED";
    public static final String DISPUTE_RESPONSE_REQUIRED = "DISPUTE_RESPONSE_REQUIRED";
    public static final String CREDENTIAL_ASSIGNED = "CREDENTIAL_ASSIGNED";
    public static final String NEW_SELLER_ORDER = "NEW_SELLER_ORDER";
    public static final String ITEM_SOLD = "ITEM_SOLD";
    public static final String POST_APPROVED = "POST_APPROVED";
    public static final String POST_REJECTED = "POST_REJECTED";
    public static final String POST_EXPIRED = "POST_EXPIRED";
    public static final String WALLET_CREDITED = "WALLET_CREDITED";
    public static final String SELLER_EARNING = "SELLER_EARNING";
    public static final String SYSTEM_ALERT = "SYSTEM_ALERT";
    public static final String VERIFICATION_REMINDER = "VERIFICATION_REMINDER";
    public static final String ADMIN_DISPUTE_REVIEW = "ADMIN_DISPUTE_REVIEW";
    public static final String HIGH_VALUE_TRANSACTION = "HIGH_VALUE_TRANSACTION";
}
