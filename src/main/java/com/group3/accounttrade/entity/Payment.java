package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a Payment transaction in the AccountTrade platform.
 * Tracks VNPAY payments including both return URL and IPN callback data.
 */
@Data
@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_status_id", nullable = false)
    private PaymentStatus paymentStatus;

    /**
     * VNPAY transaction reference (vnp_TxnRef).
     * This is our unique order number that we send to VNPAY.
     */
    @Column(name = "vnpay_txn_ref", unique = true, nullable = false, length = 100)
    private String vnpayTxnRef;

    /**
     * VNPAY transaction number (vnp_TransactionNo).
     * Returned by VNPAY after successful payment.
     */
    @Column(name = "vnpay_transaction_no", length = 100)
    private String vnpayTransactionNo;

    /**
     * VNPAY response code (vnp_ResponseCode).
     * "00" means success.
     */
    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    /**
     * Payment amount in VND.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Currency code (VND).
     */
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "VND";

    /**
     * Bank code from VNPAY (vnp_BankCode).
     */
    @Column(name = "bank_code", length = 50)
    private String bankCode;

    /**
     * Card type from VNPAY (vnp_CardType).
     */
    @Column(name = "card_type", length = 50)
    private String cardType;

    /**
     * Payment date from VNPAY (vnp_PayDate).
     */
    @Column(name = "vnpay_pay_date")
    private LocalDateTime vnpayPayDate;

    /**
     * Order info / description (vnp_OrderInfo).
     */
    @Column(name = "order_info", length = 500)
    private String orderInfo;

    /**
     * Secure hash from VNPAY for verification.
     */
    @Column(name = "secure_hash", length = 256)
    private String secureHash;

    /**
     * IP address of the buyer from VNPAY.
     */
    @Column(name = "vnpay_ip_address", length = 45)
    private String vnpayIpAddress;

    /**
     * Timestamp when payment was initiated.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when payment was completed.
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Timestamp when payment failed.
     */
    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    /**
     * Timestamp of last update.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Number of IPN callbacks received for this payment.
     */
    @Column(name = "ipn_callback_count")
    @Builder.Default
    private Integer ipnCallbackCount = 0;

    /**
     * Timestamp of the last IPN callback.
     */
    @Column(name = "last_ipn_at")
    private LocalDateTime lastIpnAt;

    /**
     * Whether the IPN was successfully processed.
     */
    @Column(name = "ipn_processed")
    @Builder.Default
    private Boolean ipnProcessed = false;

    /**
     * Raw query string from VNPAY callback for audit purposes.
     */
    @Column(name = "raw_callback_data", columnDefinition = "TEXT")
    private String rawCallbackData;

    /**
     * Error message if payment failed.
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private Long version;
}
