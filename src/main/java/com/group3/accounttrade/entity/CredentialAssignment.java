package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing the assignment of a credential to an order item.
 * Tracks when and how credentials are delivered to buyers.
 */
@Data
@Entity
@Table(name = "credential_assignments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id", nullable = false)
    private PostCredential credential;

    /**
     * Status of this assignment.
     * Values: ASSIGNED, DELIVERED, VIEWED, CONFIRMED, DISPUTED, REPLACED, REVOKED
     */
    @Column(name = "assignment_status", length = 30, nullable = false)
    private String assignmentStatus;

    /**
     * Timestamp when the credential was assigned.
     */
    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    /**
     * Timestamp when the credential was delivered (shown to buyer).
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * Timestamp when the buyer first viewed the credential.
     */
    @Column(name = "first_viewed_at")
    private LocalDateTime firstViewedAt;

    /**
     * Timestamp when the buyer confirmed the credential works.
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * Timestamp when this assignment was replaced.
     */
    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    /**
     * Timestamp when this assignment was revoked.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Reason for replacement or revocation.
     */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    /**
     * Reference to replacement assignment if this was replaced.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_assignment_id")
    private CredentialAssignment replacedByAssignment;

    /**
     * Number of times the buyer viewed this credential.
     */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * IP address of the buyer when viewing.
     */
    @Column(name = "last_view_ip", length = 45)
    private String lastViewIp;

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

    // Status constants
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_VIEWED = "VIEWED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_DISPUTED = "DISPUTED";
    public static final String STATUS_REPLACED = "REPLACED";
    public static final String STATUS_REVOKED = "REVOKED";
}
