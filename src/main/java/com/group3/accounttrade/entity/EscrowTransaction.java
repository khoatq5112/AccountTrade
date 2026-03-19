package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a transaction (movement) within the Escrow system.
 * Tracks all escrow events: creation, release, refund, freeze, partial operations.
 */
@Data
@Entity
@Table(name = "escrow_transactions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escrow_id", nullable = false)
    private Escrow escrow;

    /**
     * Type of escrow transaction.
     * Values: CREATED, RELEASED, REFUNDED, FROZEN, PARTIAL_RELEASE, PARTIAL_REFUND, UNFROZEN
     */
    @Column(name = "transaction_type", length = 30, nullable = false)
    private String transactionType;

    /**
     * Amount involved in this transaction.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Balance after this transaction.
     */
    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    /**
     * Description of the transaction.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Reference to related entity (e.g., dispute_id, refund_request_id).
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * User who initiated this transaction (could be system for auto-release).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by")
    private User initiatedBy;

    /**
     * IP address of the initiator.
     */
    @Column(name = "initiator_ip", length = 45)
    private String initiatorIp;

    /**
     * Timestamp when the transaction occurred.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Transaction type constants
    public static final String TYPE_CREATED = "CREATED";
    public static final String TYPE_RELEASED = "RELEASED";
    public static final String TYPE_REFUNDED = "REFUNDED";
    public static final String TYPE_FROZEN = "FROZEN";
    public static final String TYPE_PARTIAL_RELEASE = "PARTIAL_RELEASE";
    public static final String TYPE_PARTIAL_REFUND = "PARTIAL_REFUND";
    public static final String TYPE_UNFROZEN = "UNFROZEN";
}
