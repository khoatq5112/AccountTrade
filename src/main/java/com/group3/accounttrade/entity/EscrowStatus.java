package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing the status of an Escrow.
 * Status flow: NOT_CREATED → HOLDING → RELEASED or REFUNDED
 * Alternative paths: FROZEN, PARTIALLY_REFUNDED
 */
@Data
@Entity
@Table(name = "Escrow_Statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscrowStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status_name", length = 50, nullable = false, unique = true)
    private String statusName;

    @Column(name = "description", length = 255)
    private String description;

    // Constants for status names
    public static final String NOT_CREATED = "NOT_CREATED";
    public static final String HOLDING = "HOLDING";
    public static final String FROZEN = "FROZEN";
    public static final String RELEASED = "RELEASED";
    public static final String REFUNDED = "REFUNDED";
    public static final String PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED";
}
