package com.group3.accounttrade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for dispute data in admin dashboard.
 * Represents a dispute requiring admin attention.
 */
public record DisputeDTO(
    Long disputeId,
    String disputeNumber,
    String orderNumber,
    BigDecimal orderAmount,
    String reason,
    String buyerName,
    String sellerName,
    LocalDateTime openedAt
) {
    /**
     * Formats the order amount as Vietnamese currency.
     */
    public String getFormattedAmount() {
        if (orderAmount == null) return "0₫";
        return String.format("%,.0f₫", orderAmount);
    }

    /**
     * Gets a truncated reason for display.
     */
    public String getTruncatedReason() {
        if (reason == null) return "";
        return reason.length() > 50 ? reason.substring(0, 50) + "..." : reason;
    }
}
