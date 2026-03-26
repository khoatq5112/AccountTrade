package com.group3.accounttrade.service;

import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.CommissionConfigRepository;
import com.group3.accounttrade.repository.PlatformEarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionService {

    private final CommissionConfigRepository commissionConfigRepository;
    private final PlatformEarningRepository platformEarningRepository;

    @Value("${escrow.platform-fee-percent:5}")
    private BigDecimal defaultFeePercent;

    public BigDecimal getGlobalRate() {
        return commissionConfigRepository.findFirstByOrderByIdAsc()
                .map(CommissionConfig::getGlobalRatePercent)
                .orElse(defaultFeePercent);
    }

    public BigDecimal getEffectiveRate(Category category) {
        if (category != null && category.getCommissionRate() != null) {
            return category.getCommissionRate();
        }
        return getGlobalRate();
    }

    public BigDecimal calculateFee(BigDecimal grossAmount, BigDecimal ratePercent) {
        BigDecimal feePercent = ratePercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return grossAmount.multiply(feePercent).setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional
    public CommissionConfig updateGlobalRate(BigDecimal newRate, User updatedBy) {
        if (newRate == null || newRate.compareTo(BigDecimal.ZERO) < 0 || newRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Commission rate must be between 0 and 100");
        }

        CommissionConfig config = commissionConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> CommissionConfig.builder()
                        .minRatePercent(BigDecimal.ZERO)
                        .maxRatePercent(BigDecimal.valueOf(100))
                        .build());

        config.setGlobalRatePercent(newRate.setScale(2, RoundingMode.HALF_UP));
        config.setUpdatedBy(updatedBy);
        CommissionConfig saved = commissionConfigRepository.save(config);
        log.info("Commission rate updated to {}% by user {}", newRate, updatedBy != null ? updatedBy.getUsername() : "system");
        return saved;
    }

    @Transactional
    public void recordEarning(Escrow escrow, Order order) {
        Category category = order.getOrderItems().isEmpty() ? null
                : order.getOrderItems().get(0).getPost() != null
                ? order.getOrderItems().get(0).getPost().getCategory()
                : null;

        BigDecimal rateApplied = getEffectiveRate(category);

        PlatformEarning earning = PlatformEarning.builder()
                .escrow(escrow)
                .order(order)
                .category(category)
                .commissionAmount(escrow.getPlatformFee())
                .rateApplied(rateApplied.setScale(2, RoundingMode.HALF_UP))
                .grossAmount(escrow.getAmount())
                .build();

        platformEarningRepository.save(earning);
        log.info("Recorded platform earning of {} for order {}", escrow.getPlatformFee(), order.getOrderNumber());
    }

    @Transactional(readOnly = true)
    public CommissionConfig getConfig() {
        return commissionConfigRepository.findFirstByOrderByIdAsc().orElse(null);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTodayEarnings() {
        LocalDateTime todayStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        return platformEarningRepository.sumCommissionAmountByCreatedAtBetween(todayStart, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthEarnings() {
        LocalDateTime monthStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
        return platformEarningRepository.sumCommissionAmountByCreatedAtBetween(monthStart, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalEarnings() {
        return platformEarningRepository.sumCommissionAmountAll();
    }

    @Transactional(readOnly = true)
    public long getTotalTransactionCount() {
        return platformEarningRepository.countAll();
    }

    @Transactional(readOnly = true)
    public Page<PlatformEarning> getEarnings(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (from != null && to != null) {
            return platformEarningRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable);
        }
        return platformEarningRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
