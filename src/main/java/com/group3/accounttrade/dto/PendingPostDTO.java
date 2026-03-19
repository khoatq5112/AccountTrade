package com.group3.accounttrade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for pending post approval data.
 * Represents a post awaiting admin approval.
 */
public record PendingPostDTO(
    Integer postId,
    String title,
    String sellerName,
    BigDecimal price,
    String categoryName,
    LocalDateTime createdAt
) {
    /**
     * Formats the price as Vietnamese currency.
     */
    public String getFormattedPrice() {
        if (price == null) return "0₫";
        return String.format("%,.0f₫", price);
    }
}
