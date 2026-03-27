package com.group3.accounttrade.service.notification;

import com.group3.accounttrade.dto.NotificationDTO;
import com.group3.accounttrade.dto.UnreadCountResponse;
import com.group3.accounttrade.entity.Notification;
import com.group3.accounttrade.entity.NotificationDeliveryLog;
import com.group3.accounttrade.entity.NotificationPreference;
import com.group3.accounttrade.entity.NotificationTemplate;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.event.NotificationEvent;
import com.group3.accounttrade.repository.NotificationDeliveryLogRepository;
import com.group3.accounttrade.repository.NotificationRepository;
import com.group3.accounttrade.repository.NotificationTemplateRepository;
import com.group3.accounttrade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central notification service for in-app delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final NotificationPreferenceService preferenceService;
    private final UserRepository userRepository;

    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            createNotificationFromEvent(event);
        } catch (Exception e) {
            log.error("Notification event handling failed for {}", event.getEventType(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotificationFromEvent(NotificationEvent event) {
        try {
            log.info("Creating notification from event {}", event.getEventType());
            NotificationTemplate template = templateRepository.findByTemplateCodeAndIsActiveTrue(event.getEventType())
                    .orElse(null);

            String category = firstNonBlank(event.getNotificationCategory(),
                    template != null ? template.getNotificationCategory() : null);
            String type = firstNonBlank(template != null ? template.getNotificationType() : null, category, event.getEventType());
            String actionUrl = resolveActionUrl(event, template);

            return createNotification(
                    event.getRecipient(),
                    type,
                    category,
                    generateTitle(template, event),
                    generateMessage(template, event),
                    template != null && template.getDefaultPriority() != null
                            ? template.getDefaultPriority()
                            : event.getDefaultPriority(),
                    event.getRelatedEntityType(),
                    event.getRelatedEntityId(),
                    actionUrl,
                    event.getPayload(),
                    event.getEventType()
            );
        } catch (Exception e) {
            log.error("Failed to create notification from event {}", event.getEventType(), e);
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotification(User recipient,
                                           String notificationType,
                                           String notificationCategory,
                                           String title,
                                           String message,
                                           int priority,
                                           String relatedEntityType,
                                           Long relatedEntityId,
                                           String actionUrl) {
        return createNotification(recipient, notificationType, notificationCategory, title, message, priority,
                relatedEntityType, relatedEntityId, actionUrl, Map.of(), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification createNotification(User recipient,
                                           String notificationType,
                                           String notificationCategory,
                                           String title,
                                           String message,
                                           int priority,
                                           String relatedEntityType,
                                           Long relatedEntityId,
                                           String actionUrl,
                                           Map<String, Object> templateVariables,
                                           String templateCode) {
        try {
            if (recipient == null) {
                log.warn("Cannot create notification for null recipient");
                return null;
            }

            NotificationPreference preference = preferenceService.getOrCreateDefaultPreferences(recipient);
            boolean inAppEnabled = isChannelEnabled(preference, notificationCategory, false);
            if (!inAppEnabled) {
                log.debug("Skipping notification because all channels are disabled for user {}", recipient.getUserId());
                return null;
            }

            String normalizedActionUrl = normalizeActionUrl(recipient, relatedEntityType, relatedEntityId, actionUrl);

            Notification notification = Notification.builder()
                    .user(recipient)
                    .notificationType(notificationType)
                    .notificationCategory(notificationCategory)
                    .title(title)
                    .message(message)
                    .priority(priority)
                    .templateCode(templateCode)
                    .templateVariables(templateVariables)
                    .relatedEntityType(relatedEntityType)
                    .relatedEntityId(relatedEntityId)
                    .actionUrl(normalizedActionUrl)
                    .inAppSent(inAppEnabled)
                    .inAppSentAt(inAppEnabled ? LocalDateTime.now() : null)
                    .build();

            notification = notificationRepository.save(notification);

            if (inAppEnabled) {
                logDeliverySuccess(notification, NotificationDeliveryLog.CHANNEL_IN_APP);
            }

            return notification;
        } catch (Exception e) {
            log.error("Failed to create notification for user {}", recipient != null ? recipient.getUserId() : null, e);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotificationsForUser(Integer userId, Pageable pageable, String type, boolean unreadOnly) {
        Page<Notification> notifications;
        if (unreadOnly) {
            notifications = type != null && !type.isBlank()
                    ? notificationRepository.findByUser_UserIdAndNotificationTypeAndIsReadFalseOrderByCreatedAtDesc(userId, type, pageable)
                    : notificationRepository.findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            notifications = type != null && !type.isBlank()
                    ? notificationRepository.findByUser_UserIdAndNotificationTypeOrderByCreatedAtDesc(userId, type, pageable)
                    : notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return notifications.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public NotificationDTO getNotificationForUser(Long notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || !notification.getUser().getUserId().equals(userId)) {
            return null;
        }
        return toDTO(notification);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Integer userId) {
        long totalCount = notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
        Map<String, Long> countByType = toCountMap(notificationRepository.countUnreadByTypeAndUserId(userId));
        return UnreadCountResponse.builder()
                .totalCount(totalCount)
                .countByType(countByType)
                .build();
    }

    @Transactional
    public boolean markAsRead(Long notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || !notification.getUser().getUserId().equals(userId)) {
            return false;
        }
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        return true;
    }

    @Transactional
    public int markAllAsRead(Integer userId, String type) {
        List<Notification> unread = type != null && !type.isBlank()
                ? notificationRepository.findByUser_UserIdAndNotificationTypeAndIsReadFalse(userId, type)
                : notificationRepository.findByUser_UserIdAndIsReadFalse(userId);

        LocalDateTime now = LocalDateTime.now();
        unread.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Transactional
    public boolean archiveNotification(Long notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || !notification.getUser().getUserId().equals(userId)) {
            return false;
        }
        notification.setIsArchived(true);
        notification.setArchivedAt(LocalDateTime.now());
        notificationRepository.save(notification);
        return true;
    }

    @Transactional
    public boolean deleteNotification(Long notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || !notification.getUser().getUserId().equals(userId)) {
            return false;
        }
        notificationRepository.delete(notification);
        return true;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Map<String, Long> byChannel = toCountMap(deliveryLogRepository.countByChannel());
        byChannel.put(NotificationDeliveryLog.CHANNEL_EMAIL, 0L);
        stats.put("totalSent", notificationRepository.count());
        stats.put("byType", toCountMap(notificationRepository.countByType()));
        stats.put("byChannel", byChannel);
        stats.put("failedCount", 0L);
        stats.put("pendingEmailCount", 0L);
        return stats;
    }

    @Transactional
    public int retryFailedEmailNotifications() {
        return 0;
    }

    @Transactional
    public int broadcastNotification(String title, String message, String type, Integer priority, List<String> targetRoles) {
        List<User> recipients;
        if (targetRoles == null || targetRoles.isEmpty() || targetRoles.stream().anyMatch(role -> "ALL".equalsIgnoreCase(role))) {
            recipients = userRepository.findAll();
        } else {
            recipients = targetRoles.stream()
                    .flatMap(role -> userRepository.findByRole_RoleNameIgnoreCase(role).stream())
                    .distinct()
                    .toList();
        }

        for (User recipient : recipients) {
            createNotification(
                    recipient,
                    type,
                    Notification.TYPE_SYSTEM,
                    title,
                    message,
                    priority != null ? priority : Notification.PRIORITY_NORMAL,
                    "SYSTEM",
                    null,
                    "/",
                    Map.of("title", title, "message", message),
                    NotificationTemplate.SYSTEM_ALERT
            );
        }
        return recipients.size();
    }

    private boolean isChannelEnabled(NotificationPreference preference, String category, boolean emailChannel) {
        if (preference == null) {
            return true;
        }

        if (emailChannel) {
            return false;
        }

        if (category == null || preference.getCategoryPreferences() == null) {
            return true;
        }

        NotificationPreference.ChannelPreference channelPreference = preference.getCategoryPreferences().get(category);
        if (channelPreference == null) {
            return true;
        }
        return emailChannel ? channelPreference.isEmail() : channelPreference.isInApp();
    }

    private String generateTitle(NotificationTemplate template, NotificationEvent event) {
        if (template != null && template.getTitleTemplate() != null) {
            return processTemplate(template.getTitleTemplate(), event.getPayload());
        }
        return humanize(event.getEventType());
    }

    private String generateMessage(NotificationTemplate template, NotificationEvent event) {
        if (template != null && template.getMessageTemplate() != null) {
            return processTemplate(template.getMessageTemplate(), event.getPayload());
        }
        return "You have a new notification regarding " + humanize(event.getEventType()).toLowerCase();
    }

    private String resolveActionUrl(NotificationEvent event, NotificationTemplate template) {
        String candidate = firstNonBlank(event.getActionUrl(),
                template != null ? processTemplate(template.getDefaultActionUrlTemplate(), event.getPayload()) : null);

        if (candidate == null || event == null || event.getRecipient() == null) {
            return candidate;
        }

        if ("DISPUTE".equalsIgnoreCase(event.getRelatedEntityType()) && event.getRelatedEntityId() != null) {
            String roleName = event.getRecipient().getRole() != null ? event.getRecipient().getRole().getRoleName() : null;
            if ("Admin".equalsIgnoreCase(roleName)) {
                return "/admin/disputes/" + event.getRelatedEntityId();
            }
            if ("Seller".equalsIgnoreCase(roleName)) {
                return "/seller/disputes/" + event.getRelatedEntityId();
            }
            if ("Buyer".equalsIgnoreCase(roleName)) {
                return "/buyer/disputes/" + event.getRelatedEntityId();
            }
        }

        return candidate;
    }

    private String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            Object value = entry.getValue();
            if (value != null) {
                result = result.replace(placeholder, value.toString());
            }
        }
        return result;
    }

    private void logDeliverySuccess(Notification notification, String channel) {
        deliveryLogRepository.save(NotificationDeliveryLog.builder()
                .notification(notification)
                .channel(channel)
                .status(NotificationDeliveryLog.STATUS_SENT)
                .deliveredAt(LocalDateTime.now())
                .build());
    }

    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .notificationId(notification.getNotificationId())
                .notificationType(notification.getNotificationType())
                .notificationCategory(notification.getNotificationCategory())
                .priority(notification.getPriority())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .actionUrl(normalizeActionUrl(
                        notification.getUser(),
                        notification.getRelatedEntityType(),
                        notification.getRelatedEntityId(),
                        notification.getActionUrl()))
                .isRead(Boolean.TRUE.equals(notification.getIsRead()))
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .emailSent(Boolean.TRUE.equals(notification.getEmailSent()))
                .build();
    }

    private String normalizeActionUrl(User recipient,
                                      String relatedEntityType,
                                      Long relatedEntityId,
                                      String actionUrl) {
        if ("DISPUTE".equalsIgnoreCase(relatedEntityType) && relatedEntityId != null) {
            String roleName = recipient != null && recipient.getRole() != null
                    ? recipient.getRole().getRoleName()
                    : null;

            if ("Admin".equalsIgnoreCase(roleName)) {
                return "/admin/disputes/" + relatedEntityId;
            }
            if ("Seller".equalsIgnoreCase(roleName)) {
                return "/seller/disputes/" + relatedEntityId;
            }
            if ("Buyer".equalsIgnoreCase(roleName)) {
                return "/buyer/disputes/" + relatedEntityId;
            }
        }

        return actionUrl;
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row.length >= 2 && row[0] != null && row[1] != null) {
                counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
            }
        }
        return counts;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private String humanize(String value) {
        return value == null ? "Notification" : value.replace('_', ' ').trim();
    }
}
