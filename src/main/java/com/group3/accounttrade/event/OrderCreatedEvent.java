package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.Order;
import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a new order is placed.
 * Notifies both the buyer and seller.
 */
@Getter
public class OrderCreatedEvent extends NotificationEvent {

    private final Order order;

    public OrderCreatedEvent(Object source, Order order, User recipient) {
        super(source, "ORDER_CREATED", recipient, buildPayload(order));
        this.order = order;
    }

    private static Map<String, Object> buildPayload(Order order) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getOrderId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("itemCount", order.getOrderItems() != null ? order.getOrderItems().size() : 0);
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
