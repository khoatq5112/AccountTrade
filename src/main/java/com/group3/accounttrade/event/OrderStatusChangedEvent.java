package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when an order status changes.
 * Notifies the buyer about status updates.
 */
@Getter
public class OrderStatusChangedEvent extends NotificationEvent {

    private final Order order;
    private final String oldStatus;
    private final String newStatus;

    public OrderStatusChangedEvent(Object source, Order order, User recipient, String oldStatus, String newStatus) {
        super(source, "ORDER_STATUS_CHANGED", recipient, buildPayload(order, oldStatus, newStatus));
        this.order = order;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    private static Map<String, Object> buildPayload(Order order, String oldStatus, String newStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getOrderId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        return payload;
    }

    @Override
    public String getNotificationCategory() {
        return "ORDER";
    }

    @Override
    public int getDefaultPriority() {
        return 2; // NORMAL
    }

    @Override
    public String getActionUrl() {
        return "/orders/" + order.getOrderId();
    }

    @Override
    public String getRelatedEntityType() {
        return "ORDER";
    }

    @Override
    public Long getRelatedEntityId() {
        return order.getOrderId();
    }
}
