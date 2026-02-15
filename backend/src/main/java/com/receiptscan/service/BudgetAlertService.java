package com.receiptscan.service;

import com.receiptscan.dto.BudgetAlertResponse;
import com.receiptscan.entity.Budget;
import com.receiptscan.entity.BudgetAlert;
import com.receiptscan.entity.User;
import com.receiptscan.exception.NotFoundException;
import com.receiptscan.repository.BudgetAlertRepository;
import com.receiptscan.repository.BudgetRepository;
import com.receiptscan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetAlertService {

    private final BudgetAlertRepository budgetAlertRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;
    private final GeminiBudgetAdvisorService geminiAdvisorService;

    /**
     * Check budget thresholds and create alert if exceeded
     */
    @Transactional
    @SuppressWarnings("null")
    public BudgetAlert checkAndCreateAlert(Long userId, String category, BudgetAlert.AlertType alertType) {
        log.info("Checking budget alert for user {} in category {}", userId, category);

        User user = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        // Find active budget for this category
        Budget budget = budgetRepository.findActiveBudgetByCategory(userId, category)
            .orElse(null);

        if (budget == null) {
            log.debug("No active budget found for category: {}", category);
            return null;
        }

        // Calculate current spending
        BigDecimal currentSpending = budgetService.calculateCategorySpending(
            userId, category, budget.getPeriodType()
        );

        BigDecimal budgetLimit = budget.getLimitAmount();
        BigDecimal percentageUsed = BigDecimal.ZERO;

        if (budgetLimit.compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = currentSpending
                .multiply(BigDecimal.valueOf(100))
                .divide(budgetLimit, 2, RoundingMode.HALF_UP);
        }

        // Determine severity based on percentage used
        BudgetAlert.Severity severity = determineSeverity(percentageUsed);

        // Only create alert if threshold exceeded (80% or more)
        if (percentageUsed.compareTo(BigDecimal.valueOf(80)) < 0 && alertType == BudgetAlert.AlertType.REAL_TIME) {
            log.debug("Budget usage at {}%, below alert threshold", percentageUsed);
            return null;
        }

        // Generate AI suggestions
        List<BudgetAlert.AISuggestion> suggestions = geminiAdvisorService.generateBudgetSuggestions(
            category, currentSpending, budgetLimit, percentageUsed
        );

        // Create alert message
        String message = buildAlertMessage(category, currentSpending, budgetLimit, percentageUsed, severity);

        // Create and save alert
        BudgetAlert alert = BudgetAlert.builder()
            .user(user)
            .budget(budget)
            .alertType(alertType)
            .severity(severity)
            .category(category)
            .currentSpending(currentSpending)
            .budgetLimit(budgetLimit)
            .percentageUsed(percentageUsed)
            .message(message)
            .aiSuggestions(suggestions)
            .isRead(false)
            .build();

        BudgetAlert savedAlert = budgetAlertRepository.save(alert);
        log.info("Budget alert created: severity={}, percentage={}%", severity, percentageUsed);
        return savedAlert;
    }

    /**
     * Get all alerts for a user
     */
    @Transactional(readOnly = true)
    public List<BudgetAlertResponse> getAllAlerts(Long userId) {
        log.info("Fetching all alerts for user {}", userId);
        List<BudgetAlert> alerts = budgetAlertRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return alerts.stream()
            .map(this::toAlertResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get unread alerts for a user
     */
    @Transactional(readOnly = true)
    public List<BudgetAlertResponse> getUnreadAlerts(Long userId) {
        log.info("Fetching unread alerts for user {}", userId);
        List<BudgetAlert> alerts = budgetAlertRepository.findUnreadAlerts(userId);
        return alerts.stream()
            .map(this::toAlertResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get alerts by type
     */
    @Transactional(readOnly = true)
    public List<BudgetAlertResponse> getAlertsByType(Long userId, BudgetAlert.AlertType alertType) {
        log.info("Fetching {} alerts for user {}", alertType, userId);
        List<BudgetAlert> alerts = budgetAlertRepository.findByUserIdAndAlertTypeOrderByCreatedAtDesc(userId, alertType);
        return alerts.stream()
            .map(this::toAlertResponse)
            .collect(Collectors.toList());
    }

    /**
     * Mark alert as read
     */
    @Transactional
    public BudgetAlertResponse markAsRead(Long userId, Long alertId) {
        log.info("Marking alert {} as read for user {}", alertId, userId);

        BudgetAlert alert = budgetAlertRepository.findById(Objects.requireNonNull(alertId))
            .orElseThrow(() -> new NotFoundException("Alert not found"));

        if (!alert.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Alert does not belong to user");
        }

        alert.setIsRead(true);
        BudgetAlert updatedAlert = budgetAlertRepository.save(alert);
        return toAlertResponse(updatedAlert);
    }

    /**
     * Mark all alerts as read
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("Marking all alerts as read for user {}", userId);
        List<BudgetAlert> unreadAlerts = budgetAlertRepository.findUnreadAlerts(userId);

        unreadAlerts.forEach(alert -> alert.setIsRead(true));
        budgetAlertRepository.saveAll(unreadAlerts);

        log.info("Marked {} alerts as read", unreadAlerts.size());
    }

    /**
     * Get unread alert count
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return budgetAlertRepository.countUnreadByUserId(userId);
    }

    /**
     * Delete alert
     */
    @Transactional
    public void deleteAlert(Long userId, Long alertId) {
        log.info("Deleting alert {} for user {}", alertId, userId);

        BudgetAlert alert = budgetAlertRepository.findById(Objects.requireNonNull(alertId))
            .orElseThrow(() -> new NotFoundException("Alert not found"));

        if (!alert.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Alert does not belong to user");
        }

        budgetAlertRepository.delete(alert);
    }

    /**
     * Determine severity based on percentage used
     */
    private BudgetAlert.Severity determineSeverity(BigDecimal percentageUsed) {
        if (percentageUsed.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BudgetAlert.Severity.CRITICAL;
        } else if (percentageUsed.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return BudgetAlert.Severity.WARNING;
        } else {
            return BudgetAlert.Severity.INFO;
        }
    }

    /**
     * Build alert message
     */
    private String buildAlertMessage(String category, BigDecimal currentSpending,
                                    BigDecimal budgetLimit, BigDecimal percentageUsed,
                                    BudgetAlert.Severity severity) {
        BigDecimal remaining = budgetLimit.subtract(currentSpending);

        if (severity == BudgetAlert.Severity.CRITICAL) {
            BigDecimal overspending = currentSpending.subtract(budgetLimit);
            return String.format(
                "Budget exceeded! You've spent $%.2f on %s, which is $%.2f over your $%.2f budget (%.1f%%).",
                currentSpending, category, overspending, budgetLimit, percentageUsed
            );
        } else if (severity == BudgetAlert.Severity.WARNING) {
            return String.format(
                "Budget warning! You've spent $%.2f on %s, leaving only $%.2f of your $%.2f budget (%.1f%% used).",
                currentSpending, category, remaining, budgetLimit, percentageUsed
            );
        } else {
            return String.format(
                "Budget update: You've spent $%.2f on %s, with $%.2f remaining of your $%.2f budget (%.1f%% used).",
                currentSpending, category, remaining, budgetLimit, percentageUsed
            );
        }
    }

    /**
     * Convert entity to response DTO
     */
    private BudgetAlertResponse toAlertResponse(BudgetAlert alert) {
        return BudgetAlertResponse.builder()
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
    }
}
