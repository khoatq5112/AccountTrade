package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an Escrow holding for an Order.
 * Money is held in escrow until buyer confirmation or timeout.
 */
@Data
@Entity
@Table(name = "escrows")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Escrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escrow_id")
    private Long escrowId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escrow_status_id", nullable = false)
    private EscrowStatus escrowStatus;

    /**
     * Total amount held in escrow.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Platform fee portion of the escrow.
     */
    @Column(name = "platform_fee", precision = 19, scale = 4)
    private BigDecimal platformFee;

    /**
     * Amount to be released to seller.
     */
    @Column(name = "seller_amount", precision = 19, scale = 4)
    private BigDecimal sellerAmount;

    /**
     * Currency code (VND).
     */
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "VND";

    /**
     * Timestamp when escrow was created (payment confirmed).
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when escrow was released to seller.
     */
    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    /**
     * Timestamp when escrow was refunded to buyer.
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /**
     * Timestamp when escrow was frozen (dispute opened).
     */
    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    /**
     * Timestamp when escrow status was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Deadline for automatic release (if buyer doesn't respond).
     */
    @Column(name = "auto_release_deadline")
    private LocalDateTime autoReleaseDeadline;

    /**
     * Reason for freeze or refund if applicable.
     */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    /**
     * Admin who processed manual release/refund.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_admin_id")
    private User processedByAdmin;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private Long version;
}
