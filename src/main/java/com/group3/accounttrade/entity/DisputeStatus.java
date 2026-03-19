package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing the status of a Dispute.
 * Status flow: OPENED → UNDER_REVIEW → RESOLVED
 * Alternative path: CANCELLED
 */
@Data
@Entity
@Table(name = "Dispute_Statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status_name", length = 50, nullable = false, unique = true)
    private String statusName;

    @Column(name = "description", length = 255)
    private String description;

    // Constants for status names
    public static final String OPENED = "OPENED";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String RESOLVED = "RESOLVED";
    public static final String CANCELLED = "CANCELLED";
}
