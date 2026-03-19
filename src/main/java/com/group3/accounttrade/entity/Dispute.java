package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Enhanced Dispute entity for handling disputes between buyers and sellers.
 * Tracks the full lifecycle of a dispute from opening to resolution.
 */
@Data
@Entity
@Table(name = "disputes")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispute_id")
    private Long disputeId;

    @Column(name = "dispute_number", unique = true, nullable = false, length = 50)
    private String disputeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_status_id", nullable = false)
    private DisputeStatus disputeStatus;

    /**
     * User who opened the dispute (usually buyer).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_id", nullable = false)
    private User openedBy;

    /**
     * The other party in the dispute (usually seller).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_id", nullable = false)
    private User respondent;

    /**
     * Admin assigned to handle this dispute.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    /**
     * Type of dispute.
     * Values: INVALID_CREDENTIAL, CREDENTIAL_USED, CREDENTIAL_EXPIRED, 
     *         DESCRIPTION_MISMATCH, NON_DELIVERY, OTHER
     */
    @Column(name = "dispute_type", length = 50, nullable = false)
    private String disputeType;

    /**
     * Reason/description provided by the opener.
     */
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    /**
     * Evidence provided by the buyer (JSON or comma-separated file references).
     */
    @Column(name = "buyer_evidence", columnDefinition = "TEXT")
    private String buyerEvidence;

    /**
     * Response from the seller.
     */
    @Column(name = "seller_response", columnDefinition = "TEXT")
    private String sellerResponse;

    /**
     * Evidence provided by the seller.
     */
    @Column(name = "seller_evidence", columnDefinition = "TEXT")
    private String sellerEvidence;

    /**
     * Admin's notes (internal).
     */
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    /**
     * Resolution type if resolved.
     * Values: REFUND_TO_BUYER, RELEASE_TO_SELLER, REPLACEMENT, PARTIAL_REFUND, NO_ACTION
     */
    @Column(name = "resolution_type", length = 50)
    private String resolutionType;

    /**
     * Detailed resolution explanation.
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /**
     * Timestamp when dispute was opened.
     */
    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    /**
     * Timestamp when seller responded.
     */
    @Column(name = "seller_responded_at")
    private LocalDateTime sellerRespondedAt;

    /**
     * Deadline for seller to respond.
     */
    @Column(name = "seller_response_deadline")
    private LocalDateTime sellerResponseDeadline;

    /**
     * Timestamp when admin started review.
     */
    @Column(name = "admin_review_started_at")
    private LocalDateTime adminReviewStartedAt;

    /**
     * Timestamp when dispute was resolved.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Admin who resolved the dispute.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_admin_id")
    private User resolvedByAdmin;

    /**
     * Timestamp of last update.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private Long version;

    // Dispute type constants
    public static final String TYPE_INVALID_CREDENTIAL = "INVALID_CREDENTIAL";
    public static final String TYPE_CREDENTIAL_USED = "CREDENTIAL_USED";
    public static final String TYPE_CREDENTIAL_EXPIRED = "CREDENTIAL_EXPIRED";
    public static final String TYPE_DESCRIPTION_MISMATCH = "DESCRIPTION_MISMATCH";
    public static final String TYPE_NON_DELIVERY = "NON_DELIVERY";
    public static final String TYPE_OTHER = "OTHER";

    // Resolution type constants
    public static final String RESOLUTION_REFUND_TO_BUYER = "REFUND_TO_BUYER";
    public static final String RESOLUTION_RELEASE_TO_SELLER = "RELEASE_TO_SELLER";
    public static final String RESOLUTION_REPLACEMENT = "REPLACEMENT";
    public static final String RESOLUTION_PARTIAL_REFUND = "PARTIAL_REFUND";
    public static final String RESOLUTION_NO_ACTION = "NO_ACTION";
}
