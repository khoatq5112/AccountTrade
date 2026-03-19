package com.group3.accounttrade.dto;

import java.math.BigDecimal;

/**
 * DTO for chart data points.
 * Represents a single data point in the transaction volume chart.
 */
public record ChartDataPoint(
    String label,
    BigDecimal value
) {
    /**
     * Formats the value as Vietnamese currency.
     */
    public String getFormattedValue() {
        if (value == null) return "0₫";
        return String.format("%,.0f₫", value);
    }
}
