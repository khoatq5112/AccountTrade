package com.group3.accounttrade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for notification responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long notificationId;
    private String notificationType;
    private String notificationCategory;
    private int priority;
    private String title;
    private String message;
    private String relatedEntityType;
    private Long relatedEntityId;
    private String actionUrl;
    private boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private boolean emailSent;
}
