package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing the status of a Payment.
 * Status flow: INITIATED → PENDING → PAID or FAILED
 * Alternative paths: CALLBACK_MISMATCH, REFUNDED
 */
@Data
@Entity
@Table(name = "Payment_Statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status_name", length = 50, nullable = false, unique = true)
    private String statusName;

    @Column(name = "description", length = 255)
    private String description;

    // Constants for status names
    public static final String INITIATED = "INITIATED";
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String FAILED = "FAILED";
    public static final String CALLBACK_MISMATCH = "CALLBACK_MISMATCH";
    public static final String REFUNDED = "REFUNDED";
}
