package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a dispute is opened.
 * Notifies the buyer, seller, through potentially admins.
 */
@Getter
public class DisputeOpenedEvent extends NotificationEvent {

    private final Dispute dispute;

    public DisputeOpenedEvent(Object source, Dispute dispute, User recipient) {
        super(source, "DISPUTE_OPENED", recipient, buildPayload(dispute));
        this.dispute = dispute;
    }

    private static Map<String, Object> buildPayload(Dispute dispute) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("disputeId", dispute.getDisputeId());
        payload.put("disputeNumber", dispute.getDisputeNumber());
        payload.put("reason", dispute.getReason());
        payload.put("disputeType", dispute.getDisputeType());
        
        if (dispute.getOrder() != null) {
            payload.put("orderId", dispute.getOrder().getOrderId());
            payload.put("orderNumber", dispute.getOrder().getOrderNumber());
        }
        return payload;
    }

    @Override
    public String getNotificationCategory() {
        return "DISPUTE";
    }

    @Override
    public int getDefaultPriority() {
        return 4; // URGENT - disputes require immediate attention
    }

    @Override
    public String getActionUrl() {
        return "/disputes/" + dispute.getDisputeId();
    }

    @Override
    public String getRelatedEntityType() {
        return "DISPUTE";
    }

    @Override
    public Long getRelatedEntityId() {
        return dispute.getDisputeId();
    }
}
