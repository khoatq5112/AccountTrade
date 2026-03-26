package com.group3.accounttrade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CommissionConfigDTO(
        Long id,
        BigDecimal globalRatePercent,
        BigDecimal minRatePercent,
        BigDecimal maxRatePercent,
        LocalDateTime updatedAt,
        String updatedByUsername
) {}
