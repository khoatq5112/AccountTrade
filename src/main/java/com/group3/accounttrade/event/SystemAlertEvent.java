package com.group3.accounttrade.event;

import com.group3.accounttrade.entity.User;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Event fired for system-wide alerts.
 * Notifies all users or specific roles.
 */
@Getter
public class SystemAlertEvent extends NotificationEvent {
    private final String alertType;
    private final String targetRoles;

    public SystemAlertEvent(Object source, User recipient, String alertType, String targetRoles) {
        super(source, "SYSTEM_ALERT", recipient, buildPayload(alertType, targetRoles));
        this.alertType = alertType;
        this.targetRoles = targetRoles;
    }

    private static Map<String, Object> buildPayload(String alertType, String targetRoles) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertType", alertType);
        payload.put("targetRoles", targetRoles);
        return payload;
    }

    @Override
    public String getNotificationCategory() {
        return "SYSTEM";
    }

    @Override
    public int getDefaultPriority() {
        return switch (alertType) {
            case "URGENT" -> 4;
            case "HIGH" -> 3;
            case "NORMAL" -> 2;
            default -> 1;
        };
    }

    @Override
    public String getActionUrl() {
        return null;
    }
}
