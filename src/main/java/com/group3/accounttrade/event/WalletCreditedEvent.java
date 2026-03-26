package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.entity.WalletTransaction;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a wallet is credited.
 * Notifies the user about wallet top-up or transaction.
 */
@Getter
public class WalletCreditedEvent extends NotificationEvent {
    private final BigDecimal amount;
    private final WalletTransaction transaction;

    public WalletCreditedEvent(Object source, User recipient, BigDecimal amount, WalletTransaction transaction) {
        super(source, "WALLET_CREDITED", recipient, buildPayload(amount, transaction));
        this.amount = amount;
        this.transaction = transaction;
    }

    private static Map<String, Object> buildPayload(BigDecimal amount, WalletTransaction transaction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount);
        payload.put("transactionId", transaction.getTransactionId());
        payload.put("transactionType", transaction.getType());
        payload.put("balanceAfter", transaction.getBalanceAfter());
        payload.put("description", transaction.getDescription());
        return payload;
    }

    @Override
    public String getNotificationCategory() {
        return "WALLET";
    }

    @Override
    public int getDefaultPriority() {
        return 2; // NORMAL
    }

    @Override
    public String getActionUrl() {
        return "/wallet";
    }

    @Override
    public String getRelatedEntityType() {
        return "WALLET";
    }

    @Override
    public Long getRelatedEntityId() {
        return transaction.getTransactionId();
    }
}
