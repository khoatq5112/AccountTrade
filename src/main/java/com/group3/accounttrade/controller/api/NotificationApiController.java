package com.group3.accounttrade.controller.api;

import com.group3.accounttrade.dto.NotificationDTO;
import com.group3.accounttrade.dto.UnreadCountResponse;
import com.group3.accounttrade.entity.Notification;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/api/notifications")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDTO> notifications = notificationService.getNotificationsForUser(user.getUserId(), pageable, type, unreadOnly);
        UnreadCountResponse unreadCount = notificationService.getUnreadCount(user.getUserId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", notifications.getContent());
        response.put("page", notifications.getNumber());
        response.put("size", notifications.getSize());
        response.put("totalElements", notifications.getTotalElements());
        response.put("totalPages", notifications.getTotalPages());
        response.put("unreadCount", unreadCount.getTotalCount());
        response.put("countByType", unreadCount.getCountByType());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.getUnreadCount(user.getUserId()));
    }

    @GetMapping("/api/notifications/{id}")
    public ResponseEntity<NotificationDTO> getNotification(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        NotificationDTO notification = notificationService.getNotificationForUser(id, user.getUserId());
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/api/notifications/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        boolean success = notificationService.markAsRead(id, user.getUserId());
        return success ? ResponseEntity.ok(successBody()) : ResponseEntity.notFound().build();
    }

    @PutMapping("/api/notifications/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@RequestParam(required = false) String type) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        int updatedCount = notificationService.markAllAsRead(user.getUserId(), type);
        Map<String, Object> response = successBody();
        response.put("updatedCount", updatedCount);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/notifications/{id}/archive")
    public ResponseEntity<Map<String, Object>> archiveNotification(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        boolean success = notificationService.archiveNotification(id, user.getUserId());
        return success ? ResponseEntity.ok(successBody()) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/api/notifications/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        boolean success = notificationService.deleteNotification(id, user.getUserId());
        return success ? ResponseEntity.ok(successBody()) : ResponseEntity.notFound().build();
    }

    @GetMapping("/api/admin/notifications/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        if (getCurrentUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }
        return ResponseEntity.ok(notificationService.getAdminStats());
    }

    @PostMapping("/api/admin/notifications/retry-failed")
    public ResponseEntity<Map<String, Object>> retryFailedNotifications() {
        if (getCurrentUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        Map<String, Object> response = successBody();
        response.put("retriedCount", notificationService.retryFailedEmailNotifications());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/admin/notifications/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(@RequestBody Map<String, Object> request) {
        if (getCurrentUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("Please login first"));
        }

        @SuppressWarnings("unchecked")
        List<String> targetRoles = (List<String>) request.get("targetRoles");
        Integer priority = request.get("priority") instanceof Number number ? number.intValue() : Notification.PRIORITY_NORMAL;
        int sentCount = notificationService.broadcastNotification(
                String.valueOf(request.getOrDefault("title", "System notification")),
                String.valueOf(request.getOrDefault("message", "")),
                String.valueOf(request.getOrDefault("type", Notification.TYPE_SYSTEM)),
                priority,
                targetRoles
        );

        Map<String, Object> response = successBody();
        response.put("sentCount", sentCount);
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    private Map<String, Object> successBody() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        return response;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> response = successBody();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
