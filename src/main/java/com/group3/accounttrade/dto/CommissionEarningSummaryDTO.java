package com.group3.accounttrade.dto;

import java.math.BigDecimal;

public record CommissionEarningSummaryDTO(
        BigDecimal todayEarnings,
        BigDecimal monthEarnings,
        BigDecimal totalEarnings,
        long transactionCount
) {}
