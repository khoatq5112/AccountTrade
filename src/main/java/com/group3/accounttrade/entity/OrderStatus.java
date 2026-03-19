package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing the status of an Order.
 * Status flow: PENDING → AWAITING_PAYMENT → PAID → PROCESSING → CREDENTIAL_ASSIGNED → AWAITING_BUYER_CONFIRMATION → COMPLETED
 * Alternative paths: PAYMENT_FAILED, PAYMENT_EXPIRED, DISPUTED, CANCELLED, REFUNDED
 */
@Data
@Entity
@Table(name = "Order_Statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status_name", length = 50, nullable = false, unique = true)
    private String statusName;

    @Column(name = "description", length = 255)
    private String description;

    // Constants for status names
    public static final String PENDING = "PENDING";
    public static final String AWAITING_PAYMENT = "AWAITING_PAYMENT";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_EXPIRED = "PAYMENT_EXPIRED";
    public static final String PAID = "PAID";
    public static final String PROCESSING = "PROCESSING";
    public static final String CREDENTIAL_ASSIGNED = "CREDENTIAL_ASSIGNED";
    public static final String AWAITING_BUYER_CONFIRMATION = "AWAITING_BUYER_CONFIRMATION";
    public static final String DISPUTED = "DISPUTED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUNDED = "REFUNDED";
}
