package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.Escrow;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when an escrow is refunded.
 * Notifies the buyer about the refund.
 */
@Getter
public class EscrowRefundedEvent extends NotificationEvent {
    private final Escrow escrow;
    private final BigDecimal refundAmount;

    public EscrowRefundedEvent(Object source, Escrow escrow, User recipient, BigDecimal refundAmount) {
        super(source, "ESCROW_REFUNDED", recipient, buildPayload(escrow, refundAmount));
        this.escrow = escrow;
        this.refundAmount = refundAmount;
    }

    private static Map<String, Object> buildPayload(Escrow escrow, BigDecimal refundAmount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("escrowId", escrow.getEscrowId());
        payload.put("refundAmount", refundAmount);
        if (escrow.getOrder() != null) {
            payload.put("orderId", escrow.getOrder().getOrderId());
            payload.put("orderNumber", escrow.getOrder().getOrderNumber());
        }
        return payload;
    }

    @Override
    public String getNotificationCategory() {
        return "ESCROW";
    }

    @Override
    public int getDefaultPriority() {
        return 3; // HIGH - refunds are important
    }

    @Override
    public String getActionUrl() {
        if (escrow.getOrder() != null) {
            return "/seller/orders/" + escrow.getEscrowId();
        }
        return null;
    }
}
