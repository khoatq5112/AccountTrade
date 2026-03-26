package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.Payment;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a payment is successfully received.
 * Notifies the buyer about payment confirmation.
 */
@Getter
public class PaymentReceivedEvent extends NotificationEvent {

    private final Payment payment;

    public PaymentReceivedEvent(Object source, Payment payment, User recipient) {
        super(source, "PAYMENT_RECEIVED", recipient, buildPayload(payment));
        this.payment = payment;
    }

    private static Map<String, Object> buildPayload(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getPaymentId());
        payload.put("amount", payment.getAmount());
        payload.put("vnpayTxnRef", payment.getVnpayTxnRef());
        payload.put("vnpayTransactionNo", payment.getVnpayTransactionNo());
        payload.put("bankCode", payment.getBankCode());
        
        if (payment.getOrder() != null) {
            payload.put("orderId", payment.getOrder().getOrderId());
            payload.put("orderNumber", payment.getOrder().getOrderNumber());
        }
        return payload;
    }

    @Override
    public String getNotificationCategory() {
        return "PAYMENT";
    }

    @Override
    public int getDefaultPriority() {
        return 2; // NORMAL
    }

    @Override
    public String getActionUrl() {
        if (payment.getOrder() != null) {
            return "/orders/" + payment.getOrder().getOrderId();
        }
        return "/wallet";
    }

    @Override
    public String getRelatedEntityType() {
        return "PAYMENT";
    }

    @Override
    public Long getRelatedEntityId() {
        return payment.getPaymentId();
    }
}
