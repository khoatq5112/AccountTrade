package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a refund request for an order.
 * Tracks the full lifecycle of refund processing.
 */
@Data
@Entity
@Table(name = "refund_requests")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "refund_number", unique = true, nullable = false, length = 50)
    private String refundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id")
    private Dispute dispute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_review_id")
    private AdminReview adminReview;

    /**
     * User who requested the refund (usually buyer).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    /**
     * Admin who processed the refund.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_admin_id")
    private User processedByAdmin;

    /**
     * Type of refund.
     * Values: FULL, PARTIAL, DISPUTE_RESOLUTION, ADMIN_DECISION, SYSTEM_ERROR
     */
    @Column(name = "refund_type", length = 30, nullable = false)
    private String refundType;

    /**
     * Reason for the refund.
     */
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    /**
     * Original amount paid.
     */
    @Column(name = "original_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal originalAmount;

    /**
     * Amount to be refunded.
     */
    @Column(name = "refund_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal refundAmount;

    /**
     * Platform fee retained (if partial refund).
     */
    @Column(name = "platform_fee_retained", precision = 19, scale = 4)
    private BigDecimal platformFeeRetained;

    /**
     * Current status of the refund.
     * Values: PENDING, APPROVED, PROCESSING, COMPLETED, REJECTED, CANCELLED
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /**
     * Admin's notes (internal).
     */
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    /**
     * Reason for rejection if rejected.
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * VNPAY refund transaction reference.
     */
    @Column(name = "vnpay_refund_txn_ref", length = 100)
    private String vnpayRefundTxnRef;

    /**
     * VNPAY refund response code.
     */
    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    /**
     * Timestamp when refund was requested.
     */
    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /**
     * Timestamp when refund was approved.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Admin who approved the refund.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_admin_id")
    private User approvedByAdmin;

    /**
     * Timestamp when refund was processed.
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Timestamp when refund was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private Long version;

    // Refund type constants
    public static final String TYPE_FULL = "FULL";
    public static final String TYPE_PARTIAL = "PARTIAL";
    public static final String TYPE_DISPUTE_RESOLUTION = "DISPUTE_RESOLUTION";
    public static final String TYPE_ADMIN_DECISION = "ADMIN_DECISION";
    public static final String TYPE_SYSTEM_ERROR = "SYSTEM_ERROR";

    // Status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";
}
