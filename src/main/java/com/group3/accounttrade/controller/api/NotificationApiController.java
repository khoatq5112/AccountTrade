package com.group3.accounttrade.controller.api;

import com.group3.accounttrade.dto.NotificationDTO;
import com.group3.accounttrade.dto.UnreadCountResponse;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.UserRepository;
import com.group3.accounttrade.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String type) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                notificationService.getNotificationsForUser(
                        user.getUserId(),
                        pageable,
                        type,
                        Boolean.TRUE.equals(unreadOnly)
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(notificationService.getUnreadCount(user.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDTO> getNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        NotificationDTO notification = notificationService.getNotificationForUser(id, user.getUserId());
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(notification);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean updated = notificationService.markAsRead(id, user.getUserId());
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Integer> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        int updatedCount = notificationService.markAllAsRead(user.getUserId(), type);
        return ResponseEntity.ok(updatedCount);
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<Void> archiveNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean archived = notificationService.archiveNotification(id, user.getUserId());
        return archived ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean deleted = notificationService.deleteNotification(id, user.getUserId());
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

}

