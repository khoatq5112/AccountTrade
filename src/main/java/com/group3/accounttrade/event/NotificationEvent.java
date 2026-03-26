package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all notification events in the system.
 * Events are published using Spring's event mechanism and handled asynchronously.
 */
@Getter
public abstract class NotificationEvent extends ApplicationEvent {

    private final String eventType;
    private final User recipient;
    private final Map<String, Object> payload;
    private final LocalDateTime eventTime;

    /**
     * Create a new notification event.
     *
     * @param source    The object on which the event initially occurred
     * @param eventType The type of event (e.g., ORDER_CREATED, PAYMENT_RECEIVED)
     * @param recipient The user who should receive the notification
     * @param payload   Additional data for template processing
     */
    public NotificationEvent(Object source, String eventType, User recipient, Map<String, Object> payload) {
        super(source);
        this.eventType = eventType;
        this.recipient = recipient;
        this.payload = payload != null ? new HashMap<>(payload) : new HashMap<>();
        this.eventTime = LocalDateTime.now();
    }

    /**
     * Create a new notification event with empty payload.
     */
    public NotificationEvent(Object source, String eventType, User recipient) {
        this(source, eventType, recipient, null);
    }

    /**
     * Get a value from the payload.
     */
    public Object getPayloadValue(String key) {
        return payload.get(key);
    }

    /**
     * Get a typed value from the payload.
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayloadValue(String key, Class<T> type) {
        Object value = payload.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Add a value to the payload.
     */
    public void addPayloadValue(String key, Object value) {
        payload.put(key, value);
    }

    /**
     * Get the notification category for this event.
     * Subclasses can override to provide a specific category.
     */
    public String getNotificationCategory() {
        return null;
    }

    /**
     * Get the default priority for this event type.
     * Subclasses can override to provide a specific priority.
     * Priority levels: 1 = LOW, 2 = NORMAL, 3 = HIGH, 4 = URGENT
     */
    public int getDefaultPriority() {
        return 2; // NORMAL
    }

    /**
     * Get the action URL for the notification.
     * Subclasses can override to provide a specific URL.
     */
    public String getActionUrl() {
        return null;
    }

    /**
     * Get the related entity type for this event.
     */
    public String getRelatedEntityType() {
        return null;
    }

    /**
     * Get the related entity ID for this event.
     */
    public Long getRelatedEntityId() {
        return null;
    }
}
