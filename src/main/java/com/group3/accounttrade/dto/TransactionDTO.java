package com.group3.accounttrade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transaction display in admin dashboard.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDTO {

    private Long escrowId;
    private Long orderId;
    private String orderNumber;
    
    // Buyer information
    private Integer buyerId;
    private String buyerUsername;
    
    // Seller information
    private Integer sellerId;
    private String sellerUsername;
    
    // Product information
    private String productTitle;
    private Integer quantity;
    
    // Financial information
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal sellerAmount;
    
    // Status information
    private String status;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
    private LocalDateTime refundedAt;
}
