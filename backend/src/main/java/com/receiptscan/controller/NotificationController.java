package com.receiptscan.controller;

import com.receiptscan.dto.NotificationResponse;
import com.receiptscan.entity.User;
import com.receiptscan.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false, defaultValue = "false") boolean unreadOnly
    ) {
        log.info("Fetching notifications for user {} (unreadOnly: {})", user.getId(), unreadOnly);
        List<NotificationResponse> notifications = unreadOnly
            ? notificationService.getUnreadNotifications(user.getId())
            : notificationService.getAllNotifications(user.getId());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
        @AuthenticationPrincipal User user
    ) {
        log.info("Getting unread count for user {}", user.getId());
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Mark notification as read
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        log.info("Marking notification {} as read for user {}", id, user.getId());
        return ResponseEntity.ok(notificationService.markAsRead(user.getId(), id));
    }

    /**
     * Mark all notifications as read
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
        @AuthenticationPrincipal User user
    ) {
        log.info("Marking all notifications as read for user {}", user.getId());
        int count = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("markedAsRead", count));
    }

    /**
     * Delete a notification
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        log.info("Deleting notification {} for user {}", id, user.getId());
        notificationService.deleteNotification(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete all notifications
     * DELETE /api/notifications
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(
        @AuthenticationPrincipal User user
    ) {
        log.info("Deleting all notifications for user {}", user.getId());
        notificationService.deleteAllNotifications(user.getId());
        return ResponseEntity.noContent().build();
    }
}
