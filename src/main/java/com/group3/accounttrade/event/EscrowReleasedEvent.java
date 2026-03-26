package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.Escrow;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when an escrow is released to the seller.
 * Notifies the seller about payment receipt.
 */
@Getter
public class EscrowReleasedEvent extends NotificationEvent {

    private final Escrow escrow;
    private final BigDecimal releasedAmount;

    public EscrowReleasedEvent(Object source, Escrow escrow, User recipient, BigDecimal releasedAmount) {
        super(source, "ESCROW_RELEASED", recipient, buildPayload(escrow, releasedAmount));
        this.escrow = escrow;
        this.releasedAmount = releasedAmount;
    }

    private static Map<String, Object> buildPayload(Escrow escrow, BigDecimal releasedAmount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("escrowId", escrow.getEscrowId());
        payload.put("releasedAmount", releasedAmount);
        payload.put("originalAmount", escrow.getAmount());
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
        return 3; // HIGH - Payment received is important
    }

    @Override
    public String getActionUrl() {
        if (escrow.getOrder() != null) {
            return "/seller/orders/" + escrow.getOrder().getOrderId();
        }
        return "/seller/escrow";
    }

    @Override
    public String getRelatedEntityType() {
        return "ESCROW";
    }

    @Override
    public Long getRelatedEntityId() {
        return escrow.getEscrowId();
    }
}
