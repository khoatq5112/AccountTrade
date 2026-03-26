package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.Dispute;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a dispute is resolved.
 * Notifies the buyer and seller, and potentially admins.
 */
@Getter
public class DisputeResolvedEvent extends NotificationEvent {
    private final Dispute dispute;
    private final String resolution;

    public DisputeResolvedEvent(Object source, Dispute dispute, User recipient, String resolution) {
        super(source, "DISPUTE_RESOLVED", recipient, buildPayload(dispute, resolution));
        this.dispute = dispute;
        this.resolution = resolution;
    }

    private static Map<String, Object> buildPayload(Dispute dispute, String resolution) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("disputeId", dispute.getDisputeId());
        payload.put("disputeNumber", dispute.getDisputeNumber());
        payload.put("resolution", resolution);
        payload.put("resolutionType", dispute.getResolutionType());
        
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
        return 3; // HIGH
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
