package com.group3.accounttrade.service;

import com.group3.accounttrade.config.VnpayConfig;
import com.group3.accounttrade.entity.*;
import com.group3.accounttrade.repository.*;
import com.group3.accounttrade.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    public static final BigDecimal MIN_TOP_UP_AMOUNT = BigDecimal.valueOf(10_000);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletTopUpRepository walletTopUpRepository;
    private final VnpayConfig vnpayConfig;
    private final NotificationService notificationService;

    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUser(user).orElseGet(() -> {
            Wallet wallet = Wallet.builder().user(user).build();
            return walletRepository.save(wallet);
        });
    }

    public BigDecimal getBalance(User user) {
        return walletRepository.findByUser(user)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    public boolean hasSufficientBalance(User user, BigDecimal amount) {
        return getBalance(user).compareTo(amount) >= 0;
    }

    public BigDecimal getDeficit(User user, BigDecimal price) {
        BigDecimal balance = getBalance(user);
        BigDecimal deficit = price.subtract(balance);
        return deficit.compareTo(BigDecimal.ZERO) > 0 ? deficit : BigDecimal.ZERO;
    }

    public BigDecimal getSuggestedTopUpAmount(BigDecimal requiredAmount) {
        if (requiredAmount == null || requiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return requiredAmount.max(MIN_TOP_UP_AMOUNT);
    }

    public void validateTopUpAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_TOP_UP_AMOUNT) < 0) {
            throw new IllegalArgumentException("Số tiền nạp tối thiểu là 10,000 ₫.");
        }
    }

    @Transactional
    public WalletTransaction deductBalance(User user, BigDecimal amount, String referenceId) {
        Wallet wallet = getOrCreateWalletForUpdate(user);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Số dư không đủ. Số dư hiện tại: " + wallet.getBalance() + " VND, cần: " + amount + " VND");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransaction.TYPE_PURCHASE)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description("Thanh toán đơn hàng " + referenceId)
                .referenceId(referenceId)
                .build();
        walletTransactionRepository.save(tx);
        log.info("Deducted {} from wallet of user {}, reference: {}", amount, user.getUsername(), referenceId);
        return tx;
    }

    @Transactional
    public WalletTransaction creditBalance(User user, BigDecimal amount, String referenceId, String type) {
        Wallet wallet = getOrCreateWalletForUpdate(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        String description = switch (type) {
            case WalletTransaction.TYPE_TOP_UP -> "Nạp tiền vào ví - " + referenceId;
            case WalletTransaction.TYPE_REFUND -> "Hoàn tiền đơn hàng " + referenceId;
            case WalletTransaction.TYPE_SELLER_EARNING -> "Nhận tiền bán hàng - " + referenceId;
            case WalletTransaction.TYPE_PLATFORM_COMMISSION -> "Nhận hoa hồng nền tảng - " + referenceId;
            default -> "Cộng tiền vào ví - " + referenceId;
        };

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .type(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .referenceId(referenceId)
                .build();
        walletTransactionRepository.save(tx);
        notificationService.createNotification(
                user,
                Notification.TYPE_WALLET,
                NotificationPreference.CATEGORY_WALLET,
                buildWalletNotificationTitle(type, amount),
                description,
                Notification.PRIORITY_NORMAL,
                "WALLET_TRANSACTION",
                tx.getTransactionId(),
                "/buyer/wallet"
        );
        log.info("Credited {} to wallet of user {}, type: {}, reference: {}", amount, user.getUsername(), type, referenceId);
        return tx;
    }

    @Transactional
    public String initiateTopUp(User user, BigDecimal amount, BigDecimal requiredAmount, Integer pendingPostId,
                                HttpServletRequest request) {
        validateTopUpAmount(amount);
        if (!vnpayConfig.isConfigured()) {
            throw new IllegalStateException("VNPAY chưa được cấu hình");
        }
        Wallet wallet = getOrCreateWallet(user);

        String txnRef = "TU-" + System.currentTimeMillis() + "-" + String.format("%04d", new Random().nextInt(10000));

        WalletTopUp topUp = WalletTopUp.builder()
                .wallet(wallet)
                .amount(amount)
                .requiredAmount(requiredAmount)
                .vnpayTxnRef(txnRef)
                .pendingPostId(pendingPostId)
                .status(WalletTopUp.STATUS_PENDING)
                .build();
        walletTopUpRepository.save(topUp);

        String paymentUrl = buildTopUpPaymentUrl(txnRef, amount, request);
        log.info("Initiated top-up for user {} amount {} txnRef {}", user.getUsername(), amount, txnRef);
        return paymentUrl;
    }

    @Transactional
    public WalletTopUp confirmTopUp(String vnpayTxnRef, String vnpayTransactionNo) {
        WalletTopUp topUp = walletTopUpRepository.findByVnpayTxnRef(vnpayTxnRef)
                .orElseThrow(() -> new IllegalArgumentException("Top-up not found: " + vnpayTxnRef));

        if (WalletTopUp.STATUS_COMPLETED.equals(topUp.getStatus())) {
            log.info("Top-up already completed: {}", vnpayTxnRef);
            return topUp;
        }

        topUp.setVnpayTransactionNo(vnpayTransactionNo);
        topUp.setStatus(WalletTopUp.STATUS_COMPLETED);
        walletTopUpRepository.save(topUp);

        User user = topUp.getWallet().getUser();
        creditBalance(user, topUp.getAmount(), vnpayTxnRef, WalletTransaction.TYPE_TOP_UP);
        log.info("Confirmed top-up {} for user {}", vnpayTxnRef, user.getUsername());
        return topUp;
    }

    @Transactional
    public void failTopUp(String vnpayTxnRef) {
        walletTopUpRepository.findByVnpayTxnRef(vnpayTxnRef).ifPresent(topUp -> {
            if (WalletTopUp.STATUS_PENDING.equals(topUp.getStatus())) {
                topUp.setStatus(WalletTopUp.STATUS_FAILED);
                walletTopUpRepository.save(topUp);
            }
        });
    }

    public List<WalletTransaction> getTransactionHistory(User user) {
        return walletRepository.findByUser(user)
                .map(walletTransactionRepository::findByWalletOrderByCreatedAtDesc)
                .orElse(List.of());
    }

    public List<WalletTopUp> getTopUpHistory(User user) {
        return walletTopUpRepository.findByWallet_User_UserIdOrderByCreatedAtDesc(user.getUserId());
    }

    public Integer getPendingPostIdForTopUp(String vnpayTxnRef) {
        return walletTopUpRepository.findByVnpayTxnRef(vnpayTxnRef)
                .map(WalletTopUp::getPendingPostId)
                .orElse(null);
    }

    public BigDecimal getMinimumTopUpAmount() {
        return MIN_TOP_UP_AMOUNT;
    }

    private Wallet getOrCreateWalletForUpdate(User user) {
        return walletRepository.findByUserForUpdate(user).orElseGet(() -> getOrCreateWallet(user));
    }

    private String buildTopUpPaymentUrl(String txnRef, BigDecimal amount, HttpServletRequest request) {
        String orderInfo = "Nap tien vi TrustBridge " + txnRef;
        String amountStr = vnpayConfig.formatAmountForVnpay(amount.doubleValue());
        String ipAddress = vnpayConfig.getClientIpAddress(request);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpayConfig.getVersion());
        vnpParams.put("vnp_Command", VnpayConfig.COMMAND_PAY);
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", amountStr);
        vnpParams.put("vnp_CurrCode", VnpayConfig.CURRENCY_VND);
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", VnpayConfig.LOCALE_VN);
        vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", vnpayConfig.generateTimestamp());
        vnpParams.put("vnp_ExpireDate", vnpayConfig.generateExpireTimestamp());

        String secureHash = vnpayConfig.generateSecureHash(vnpParams);
        vnpParams.put("vnp_SecureHash", secureHash);

        StringBuilder paymentUrl = new StringBuilder(vnpayConfig.getPayUrl()).append("?");
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        for (int i = 0; i < fieldNames.size(); i++) {
            String key = fieldNames.get(i);
            String val = vnpParams.get(key);
            if (val != null && !val.isEmpty()) {
                if (i > 0) paymentUrl.append("&");
                paymentUrl.append(key).append("=").append(URLEncoder.encode(val, StandardCharsets.UTF_8));
            }
        }
        return paymentUrl.toString();
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }

    private String buildWalletNotificationTitle(String type, BigDecimal amount) {
        return switch (type) {
            case WalletTransaction.TYPE_TOP_UP -> "Ví của bạn vừa được nạp thêm " + amount;
            case WalletTransaction.TYPE_REFUND -> "Bạn đã nhận hoàn tiền " + amount;
            case WalletTransaction.TYPE_SELLER_EARNING -> "Bạn vừa nhận tiền bán hàng " + amount;
            case WalletTransaction.TYPE_PLATFORM_COMMISSION -> "Bạn vừa nhận hoa hồng nền tảng " + amount;
            default -> "Ví của bạn vừa được cộng tiền";
        };
    }
}
