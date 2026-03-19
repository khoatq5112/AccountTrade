package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing an individual item within an Order.
 * Each OrderItem links to a Post and includes the assigned credential.
 */
@Data
@Entity
@Table(name = "order_items")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /**
     * The credential assigned to this order item.
     * Can be null if not yet assigned.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_credential_id")
    private PostCredential assignedCredential;

    /**
     * Price at the time of purchase (snapshot).
     */
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    /**
     * Platform fee for this item.
     */
    @Column(name = "platform_fee", precision = 19, scale = 4)
    private BigDecimal platformFee;

    /**
     * Seller earnings for this item (unit_price - platform_fee).
     */
    @Column(name = "seller_earnings", precision = 19, scale = 4)
    private BigDecimal sellerEarnings;

    /**
     * Title of the post at the time of purchase (snapshot).
     */
    @Column(name = "post_title_snapshot", nullable = false, length = 255)
    private String postTitleSnapshot;

    /**
     * Status of this individual item.
     * Can be: PENDING, CREDENTIAL_ASSIGNED, DELIVERED, DISPUTED, COMPLETED, REFUNDED
     */
    @Column(name = "item_status", length = 50, nullable = false)
    @Builder.Default
    private String itemStatus = "PENDING";

    /**
     * Timestamp when credential was assigned.
     */
    @Column(name = "credential_assigned_at")
    private LocalDateTime credentialAssignedAt;

    /**
     * Timestamp when buyer confirmed this item.
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * Timestamp when this item was completed.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Timestamp when this item was refunded.
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /**
     * Reason for refund if applicable.
     */
    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private Long version;

    // Constants for item status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CREDENTIAL_ASSIGNED = "CREDENTIAL_ASSIGNED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_DISPUTED = "DISPUTED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REFUNDED = "REFUNDED";
}
