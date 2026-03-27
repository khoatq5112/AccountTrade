package com.group3.accounttrade.service;

import com.group3.accounttrade.config.VnpayConfig;
import com.group3.accounttrade.repository.WalletRepository;
import com.group3.accounttrade.repository.WalletTopUpRepository;
import com.group3.accounttrade.repository.WalletTransactionRepository;
import com.group3.accounttrade.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private WalletTopUpRepository walletTopUpRepository;

    @Mock
    private VnpayConfig vnpayConfig;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WalletService walletService;

    @Test
    void validateTopUpAmountRejectsAmountBelowMinimum() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.validateTopUpAmount(BigDecimal.valueOf(10_000)));

        assertEquals("Số tiền nạp tối thiểu là 20,000 ₫.", exception.getMessage());
    }

    @Test
    void validateTopUpAmountRejectsAmountAboveMaximum() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> walletService.validateTopUpAmount(BigDecimal.valueOf(6_000_000)));

        assertEquals("Số tiền nạp tối đa cho mỗi lần là 5,000,000 ₫.", exception.getMessage());
    }

    @Test
    void getSuggestedTopUpAmountCapsAtMaximumLimit() {
        BigDecimal suggestedAmount = walletService.getSuggestedTopUpAmount(BigDecimal.valueOf(7_500_000));

        assertEquals(BigDecimal.valueOf(5_000_000), suggestedAmount);
    }
}
