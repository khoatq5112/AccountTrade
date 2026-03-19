package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an admin review for manual intervention points.
 * Tracks admin actions on orders, payments, disputes, escrow, etc.
 */
@Data
@Entity
@Table(name = "admin_reviews")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    /**
     * Type of review.
     * Values: PAYMENT_MISMATCH, NO_CREDENTIAL, DUPLICATE_CREDENTIAL, 
     *         INVALID_CREDENTIAL_CLAIM, DISPUTE, REFUND, SUSPICIOUS_ACTIVITY,
     *         SELLER_NON_RESPONSE, FORCED_RELEASE, ACCOUNT_REPLACEMENT
     */
    @Column(name = "review_type", length = 50, nullable = false)
    private String reviewType;

    /**
     * Related entity type (Order, Payment, Dispute, etc.).
     */
    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    /**
     * ID of the related entity.
     */
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    /**
     * Admin assigned to this review.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    /**
     * Admin who completed the review.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedByAdmin;

    /**
     * Priority level (1=low, 2=normal, 3=high, 4=critical).
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 2;

    /**
     * Current status of the review.
     * Values: PENDING, IN_PROGRESS, RESOLVED, ESCALATED
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /**
     * Description of the issue requiring review.
     */
    @Column(name = "issue_description", columnDefinition = "TEXT", nullable = false)
    private String issueDescription;

    /**
     * System-detected flags or warnings.
     */
    @Column(name = "system_flags", columnDefinition = "TEXT")
    private String systemFlags;

    /**
     * Admin's findings during review.
     */
    @Column(name = "findings", columnDefinition = "TEXT")
    private String findings;

    /**
     * Action taken by admin.
     * Values: APPROVE, REJECT, REFUND, RELEASE_ESCROW, REPLACE_CREDENTIAL,
     *         SUSPEND_SELLER, SUSPEND_BUYER, ESCALATE, NO_ACTION
     */
    @Column(name = "action_taken", length = 50)
    private String actionTaken;

    /**
     * Detailed notes about the action taken.
     */
    @Column(name = "action_notes", columnDefinition = "TEXT")
    private String actionNotes;

    /**
     * Whether the review requires senior admin approval.
     */
    @Column(name = "requires_approval")
    @Builder.Default
    private Boolean requiresApproval = false;

    /**
     * Senior admin who approved the action.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_admin_id")
    private User approvedByAdmin;

    /**
     * Timestamp when the review was approved.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Timestamp when review was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when review was assigned.
     */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    /**
     * SLA deadline for completing the review.
     */
    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    /**
     * Timestamp when review was started.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Timestamp when review was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Review type constants
    public static final String TYPE_PAYMENT_MISMATCH = "PAYMENT_MISMATCH";
    public static final String TYPE_NO_CREDENTIAL = "NO_CREDENTIAL";
    public static final String TYPE_DUPLICATE_CREDENTIAL = "DUPLICATE_CREDENTIAL";
    public static final String TYPE_INVALID_CREDENTIAL_CLAIM = "INVALID_CREDENTIAL_CLAIM";
    public static final String TYPE_DISPUTE = "DISPUTE";
    public static final String TYPE_REFUND = "REFUND";
    public static final String TYPE_SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY";
    public static final String TYPE_SELLER_NON_RESPONSE = "SELLER_NON_RESPONSE";
    public static final String TYPE_FORCED_RELEASE = "FORCED_RELEASE";
    public static final String TYPE_ACCOUNT_REPLACEMENT = "ACCOUNT_REPLACEMENT";

    // Status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_ESCALATED = "ESCALATED";

    // Action constants
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_REFUND = "REFUND";
    public static final String ACTION_RELEASE_ESCROW = "RELEASE_ESCROW";
    public static final String ACTION_REPLACE_CREDENTIAL = "REPLACE_CREDENTIAL";
    public static final String ACTION_SUSPEND_SELLER = "SUSPEND_SELLER";
    public static final String ACTION_SUSPEND_BUYER = "SUSPEND_BUYER";
    public static final String ACTION_ESCALATE = "ESCALATE";
    public static final String ACTION_NO_ACTION = "NO_ACTION";

    // Priority constants
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_HIGH = 3;
    public static final int PRIORITY_CRITICAL = 4;
}
