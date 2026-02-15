package com.receiptscan.controller;

import com.receiptscan.dto.BudgetAlertResponse;
import com.receiptscan.entity.BudgetAlert;
import com.receiptscan.entity.User;
import com.receiptscan.service.BudgetAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class BudgetAlertController {

    private final BudgetAlertService budgetAlertService;

    @GetMapping
    public ResponseEntity<List<BudgetAlertResponse>> getAllAlerts(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) Boolean unreadOnly,
        @RequestParam(required = false) String alertType
    ) {
        try {
            List<BudgetAlertResponse> alerts;

            if (unreadOnly != null && unreadOnly) {
                alerts = budgetAlertService.getUnreadAlerts(user.getId());
            } else if (alertType != null) {
                BudgetAlert.AlertType type = BudgetAlert.AlertType.valueOf(alertType.toUpperCase());
                alerts = budgetAlertService.getAlertsByType(user.getId(), type);
            } else {
                alerts = budgetAlertService.getAllAlerts(user.getId());
            }

            return ResponseEntity.ok(alerts);
        } catch (RuntimeException e) {
            log.error("Error fetching alerts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
        @AuthenticationPrincipal User user
    ) {
        try {
            long count = budgetAlertService.getUnreadCount(user.getId());
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (RuntimeException e) {
            log.error("Error fetching unread count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}/mark-read")
    public ResponseEntity<BudgetAlertResponse> markAsRead(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        try {
            BudgetAlertResponse response = budgetAlertService.markAsRead(user.getId(), id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error marking alert as read", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(
        @AuthenticationPrincipal User user
    ) {
        try {
            budgetAlertService.markAllAsRead(user.getId());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error marking all alerts as read", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        try {
            budgetAlertService.deleteAlert(user.getId(), id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting alert", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/check/{category}")
    public ResponseEntity<BudgetAlertResponse> checkBudget(
        @AuthenticationPrincipal User user,
        @PathVariable String category
    ) {
        try {
            BudgetAlert alert = budgetAlertService.checkAndCreateAlert(
                user.getId(),
                category,
                BudgetAlert.AlertType.ON_DEMAND
            );

            if (alert == null) {
                return ResponseEntity.ok().build();
            }

            BudgetAlertResponse response = BudgetAlertResponse.builder()
                .id(alert.getId())
                .budgetId(alert.getBudget() != null ? alert.getBudget().getId() : null)
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity())
                .category(alert.getCategory())
                .currentSpending(alert.getCurrentSpending())
                .budgetLimit(alert.getBudgetLimit())
                .percentageUsed(alert.getPercentageUsed())
                .message(alert.getMessage())
                .aiSuggestions(alert.getAiSuggestions())
                .isRead(alert.getIsRead())
                .createdAt(alert.getCreatedAt())
                .build();

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error checking budget", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
