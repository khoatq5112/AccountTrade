package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an Order in the AccountTrade platform.
 * An Order is created when a buyer initiates checkout and progresses through
 * various statuses until completion or cancellation.
 */
@Data
@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_status_id", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "platform_fee", precision = 19, scale = 4)
    private BigDecimal platformFee;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "buyer_notes", columnDefinition = "TEXT")
    private String buyerNotes;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    /**
     * Timestamp when the order was placed.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the order was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Timestamp when payment was completed.
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Timestamp when credentials were assigned to the buyer.
     */
    @Column(name = "credential_assigned_at")
    private LocalDateTime credentialAssignedAt;

    /**
     * Timestamp when the buyer confirmed receipt.
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * Timestamp when the order was completed (escrow released).
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Timestamp when the order was cancelled (if applicable).
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Reason for cancellation if the order was cancelled.
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * Deadline for buyer confirmation (auto-release escrow after this).
     * Set when credentials are assigned.
     */
    @Column(name = "confirmation_deadline")
    private LocalDateTime confirmationDeadline;

    /**
     * IP address of the buyer when placing the order.
     */
    @Column(name = "buyer_ip_address", length = 45)
    private String buyerIpAddress;

    /**
     * User agent of the buyer's browser.
     */
    @Column(name = "buyer_user_agent", length = 500)
    private String buyerUserAgent;

    /**
     * Items in this order.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private Long version;
}
