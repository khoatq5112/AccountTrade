package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * System-wide audit log for tracking all important actions and events.
 * Used for security verification, compliance, and troubleshooting.
 */
@Data
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    /**
     * Type of audit event.
     * Values: ORDER, PAYMENT, ESCROW, CREDENTIAL, DISPUTE, USER, ADMIN, SYSTEM
     */
    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    /**
     * Specific action that occurred.
     * Examples: ORDER_CREATED, PAYMENT_RECEIVED, ESCROW_RELEASED, 
     *           CREDENTIAL_ASSIGNED, DISPUTE_OPENED, etc.
     */
    @Column(name = "action", length = 100, nullable = false)
    private String action;

    /**
     * Entity type affected (e.g., Order, Payment, Escrow, Dispute).
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * ID of the affected entity.
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * User who performed the action (null for system actions).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    /**
     * Role of the user at the time of action.
     * Values: BUYER, SELLER, ADMIN, SYSTEM
     */
    @Column(name = "performer_role", length = 20)
    private String performerRole;

    /**
     * Description of what happened.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Previous state (JSON representation).
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * New state (JSON representation).
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * IP address of the requester.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Request URL that triggered the action.
     */
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    /**
     * HTTP method of the request.
     */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /**
     * Whether this action was successful.
     */
    @Column(name = "success")
    @Builder.Default
    private Boolean success = true;

    /**
     * Error message if action failed.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Additional metadata (JSON format).
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Event type constants
    public static final String EVENT_ORDER = "ORDER";
    public static final String EVENT_PAYMENT = "PAYMENT";
    public static final String EVENT_ESCROW = "ESCROW";
    public static final String EVENT_CREDENTIAL = "CREDENTIAL";
    public static final String EVENT_DISPUTE = "DISPUTE";
    public static final String EVENT_USER = "USER";
    public static final String EVENT_ADMIN = "ADMIN";
    public static final String EVENT_SYSTEM = "SYSTEM";

    // Performer role constants
    public static final String ROLE_BUYER = "BUYER";
    public static final String ROLE_SELLER = "SELLER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SYSTEM = "SYSTEM";
}
