package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a message in a dispute thread.
 * Both parties (buyer, seller) and admins can send messages.
 */
@Data
@Entity
@Table(name = "dispute_messages")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Role of the sender at the time of message.
     * Values: BUYER, SELLER, ADMIN, SYSTEM
     */
    @Column(name = "sender_role", length = 20, nullable = false)
    private String senderRole;

    /**
     * Content of the message.
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Whether this message is internal (admin-only visible).
     */
    @Column(name = "is_internal")
    @Builder.Default
    private Boolean isInternal = false;

    /**
     * Whether this message contains evidence/attachment reference.
     */
    @Column(name = "has_attachment")
    @Builder.Default
    private Boolean hasAttachment = false;

    /**
     * Path to attachment if any.
     */
    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    /**
     * IP address of the sender.
     */
    @Column(name = "sender_ip", length = 45)
    private String senderIp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the message was read by the other party.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // Sender role constants
    public static final String ROLE_BUYER = "BUYER";
    public static final String ROLE_SELLER = "SELLER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SYSTEM = "SYSTEM";
}
