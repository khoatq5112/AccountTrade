package com.group3.accounttrade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlatformEarningDTO(
        Long id,
        String orderNumber,
        String categoryName,
        BigDecimal grossAmount,
        BigDecimal commissionAmount,
        BigDecimal rateApplied,
        LocalDateTime createdAt
) {}
