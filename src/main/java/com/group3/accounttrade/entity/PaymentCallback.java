package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a Payment Callback from VNPAY.
 * Stores all callback data for audit and debugging purposes.
 * Supports both Return URL (frontend) and IPN (server-to-server) callbacks.
 */
@Data
@Entity
@Table(name = "payment_callbacks")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "callback_id")
    private Long callbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    /**
     * VNPAY transaction reference.
     */
    @Column(name = "vnpay_txn_ref", length = 100)
    private String vnpayTxnRef;

    /**
     * Type of callback: RETURN_URL or IPN.
     */
    @Column(name = "callback_type", length = 20, nullable = false)
    private String callbackType;

    /**
     * Raw query string from VNPAY callback.
     */
    @Column(name = "raw_query_string", columnDefinition = "TEXT")
    private String rawQueryString;

    /**
     * VNPAY response code.
     */
    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    /**
     * VNPAY transaction number.
     */
    @Column(name = "vnpay_transaction_no", length = 100)
    private String vnpayTransactionNo;

    /**
     * Amount from callback.
     */
    @Column(name = "amount")
    private Long amount;

    /**
     * Secure hash received from VNPAY.
     */
    @Column(name = "received_hash", length = 256)
    private String receivedHash;

    /**
     * Secure hash computed locally for verification.
     */
    @Column(name = "computed_hash", length = 256)
    private String computedHash;

    /**
     * Whether the hash verification passed.
     */
    @Column(name = "hash_valid")
    private Boolean hashValid;

    /**
     * Whether this callback was successfully processed.
     */
    @Column(name = "processed")
    @Builder.Default
    private Boolean processed = false;

    /**
     * Processing result message.
     */
    @Column(name = "processing_result", length = 500)
    private String processingResult;

    /**
     * IP address of the callback source.
     */
    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    /**
     * User agent of the callback source (for return URL).
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Timestamp when the callback was received.
     */
    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    /**
     * Timestamp when the callback was processed.
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Constants for callback type
    public static final String TYPE_RETURN_URL = "RETURN_URL";
    public static final String TYPE_IPN = "IPN";
}
