package com.receiptscan.service;

import com.receiptscan.dto.NotificationResponse;
import com.receiptscan.entity.AppNotification;
import com.receiptscan.entity.User;
import com.receiptscan.exception.BadRequestException;
import com.receiptscan.exception.NotFoundException;
import com.receiptscan.repository.AppNotificationRepository;
import com.receiptscan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications(Long userId) {
        log.info("Fetching all notifications for user {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        log.info("Fetching unread notifications for user {}", userId);
        return notificationRepository.findUnreadByUserId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        log.info("Marking notification {} as read for user {}", notificationId, userId);

        AppNotification notification = notificationRepository.findById(Objects.requireNonNull(notificationId))
            .orElseThrow(() -> new NotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new BadRequestException("Notification does not belong to user");
        }

        notification.setIsRead(true);
        AppNotification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user {}", userId);
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        log.info("Deleting notification {} for user {}", notificationId, userId);

        AppNotification notification = notificationRepository.findById(Objects.requireNonNull(notificationId))
            .orElseThrow(() -> new NotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new BadRequestException("Notification does not belong to user");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllNotifications(Long userId) {
        log.info("Deleting all notifications for user {}", userId);
        notificationRepository.deleteAllByUserId(userId);
    }

    /**
     * Create a notification for a user
     */
    @Transactional
    @SuppressWarnings("null")
    public NotificationResponse createNotification(Long userId, AppNotification.NotificationType type,
                                                    String title, String message,
                                                    String relatedEntityType, Long relatedEntityId) {
        log.info("Creating {} notification for user {}: {}", type, userId, title);

        User user = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        AppNotification notification = AppNotification.builder()
            .user(user)
            .type(type)
            .title(title)
            .message(message)
            .relatedEntityType(relatedEntityType)
            .relatedEntityId(relatedEntityId)
            .isRead(false)
            .build();

        AppNotification saved = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", saved.getId());
        return toResponse(saved);
    }

    private NotificationResponse toResponse(AppNotification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .type(notification.getType())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .relatedEntityType(notification.getRelatedEntityType())
            .relatedEntityId(notification.getRelatedEntityId())
            .isRead(notification.getIsRead())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
