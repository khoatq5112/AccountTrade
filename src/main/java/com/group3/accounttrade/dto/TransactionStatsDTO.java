package com.group3.accounttrade.dto;

import java.math.BigDecimal;

/**
 * DTO for transaction statistics in admin dashboard.
 */
public record TransactionStatsDTO(
    long totalTransactions,
    long successfulTransactions,
    long pendingTransactions,
    long refundedTransactions,
    long frozenTransactions,
    BigDecimal totalVolume,
    BigDecimal escrowBalance,
    BigDecimal platformRevenue
) {
    public static TransactionStatsDTO empty() {
        return new TransactionStatsDTO(0, 0, 0, 0, 0, 
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
