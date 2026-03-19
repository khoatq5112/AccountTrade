package com.group3.accounttrade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transaction history display in admin dashboard.
 * Unified view of escrow transactions, payments, and refunds.
 */
public record TransactionDTO(
    String transactionId,       // Format: TXN-TYPE-ID (e.g., TXN-ESCROW-123)
    String transactionType,     // ESCROW_CREATED, PAYMENT_SUCCESS, etc.
    String category,            // ESCROW, PAYMENT, REFUND, ORDER
    String status,              // Current status
    BigDecimal amount,
    UserInfo buyerInfo,
    UserInfo sellerInfo,
    OrderInfo orderInfo,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String description
) {
    public record UserInfo(
        Integer userId,
        String username,
        String email
    ) {}
    
    public record OrderInfo(
        Long orderId,
        String orderNumber,
        Integer quantity
    ) {}
    
    // Category constants
    public static final String CATEGORY_ESCROW = "ESCROW";
    public static final String CATEGORY_PAYMENT = "PAYMENT";
    public static final String CATEGORY_REFUND = "REFUND";
    public static final String CATEGORY_ORDER = "ORDER";
    
    // Transaction type constants
    public static final String TYPE_ESCROW_CREATED = "ESCROW_CREATED";
    public static final String TYPE_ESCROW_RELEASED = "ESCROW_RELEASED";
    public static final String TYPE_ESCROW_REFUNDED = "ESCROW_REFUNDED";
    public static final String TYPE_ESCROW_FROZEN = "ESCROW_FROZEN";
    public static final String TYPE_ESCROW_UNFROZEN = "ESCROW_UNFROZEN";
    public static final String TYPE_PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String TYPE_PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String TYPE_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String TYPE_REFUND_APPROVED = "REFUND_APPROVED";
    public static final String TYPE_REFUND_PENDING = "REFUND_PENDING";
    public static final String TYPE_REFUND_COMPLETED = "REFUND_COMPLETED";
    
    // Status constants
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PROCESSING = "PROCESSING";
}
