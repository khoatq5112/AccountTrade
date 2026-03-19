package com.group3.accounttrade.dto;

import java.math.BigDecimal;

/**
 * DTO for admin dashboard statistics.
 * Contains aggregated platform metrics for the admin overview.
 */
public record AdminDashboardStats(
    BigDecimal dailyTransactionVolume,
    double dailyVolumeChangePercent,
    BigDecimal escrowHoldings,
    long escrowTransactionCount,
    BigDecimal monthlyFeeRevenue,
    double monthlyRevenueChangePercent,
    long pendingDisputeCount,
    long pendingApprovalCount
) {
    /**
     * Formats the daily transaction volume as Vietnamese currency.
     */
    public String getFormattedDailyVolume() {
        return formatCurrency(dailyTransactionVolume);
    }

    /**
     * Formats the escrow holdings as Vietnamese currency.
     */
    public String getFormattedEscrowHoldings() {
        return formatCurrency(escrowHoldings);
    }

    /**
     * Formats the monthly fee revenue as Vietnamese currency.
     */
    public String getFormattedMonthlyRevenue() {
        return formatCurrency(monthlyFeeRevenue);
    }

    /**
     * Gets the trend direction for daily volume.
     * @return "up", "down", or "neutral"
     */
    public String getVolumeTrendDirection() {
        if (dailyVolumeChangePercent > 0) return "up";
        if (dailyVolumeChangePercent < 0) return "down";
        return "neutral";
    }

    /**
     * Gets the trend direction for monthly revenue.
     * @return "up", "down", or "neutral"
     */
    public String getRevenueTrendDirection() {
        if (monthlyRevenueChangePercent > 0) return "up";
        if (monthlyRevenueChangePercent < 0) return "down";
        return "neutral";
    }

    /**
     * Gets the total alerts count (disputes + pending approvals).
     */
    public long getTotalAlertsCount() {
        return pendingDisputeCount + pendingApprovalCount;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0₫";
        return String.format("%,.0f₫", amount);
    }
}
