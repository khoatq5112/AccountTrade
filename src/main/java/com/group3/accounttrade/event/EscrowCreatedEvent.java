package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.Escrow;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when an escrow is created.
 * Notifies both buyer and seller.
 */
@Getter
public class EscrowCreatedEvent extends NotificationEvent {
    private final Escrow escrow;

    public EscrowCreatedEvent(Object source, Escrow escrow, User recipient) {
        super(source, "ESCROW_CREATED", recipient, buildPayload(escrow));
        this.escrow = escrow;
    }

    private static Map<String, Object> buildPayload(Escrow escrow) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("escrowId", escrow.getEscrowId());
        payload.put("amount", escrow.getAmount());
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
        return 2; // NORMAL
    }

    @Override
    public String getActionUrl() {
        if (escrow.getOrder() != null) {
            return "/seller/orders/" + escrow.getEscrowId();
        }
        return null;
    }
}
