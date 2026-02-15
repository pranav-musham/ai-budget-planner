package com.receiptscan.service;

import com.receiptscan.entity.Budget;
import com.receiptscan.entity.BudgetAlert;
import com.receiptscan.entity.User;
import com.receiptscan.repository.BudgetRepository;
import com.receiptscan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled tasks for budget monitoring
 * - Weekly budget summary (every Sunday at 8 PM)
 * - Monthly budget summary (1st day of month at 9 AM)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetAlertService budgetAlertService;

    /**
     * Weekly budget summary - runs every Sunday at 8:00 PM
     * Checks all active weekly budgets and creates summary alerts
     */
    @Scheduled(cron = "0 0 20 * * SUN")
    public void generateWeeklySummaries() {
        log.info("Starting weekly budget summary task");

        try {
            List<User> users = userRepository.findAll();
            int alertsCreated = 0;

            for (User user : users) {
                List<Budget> weeklyBudgets = budgetRepository.findByUserIdAndIsActive(
                    user.getId(), true
                ).stream()
                .filter(b -> b.getPeriodType() == Budget.PeriodType.WEEKLY)
                .toList();

                for (Budget budget : weeklyBudgets) {
                    try {
                        BudgetAlert alert = budgetAlertService.checkAndCreateAlert(
                            user.getId(),
                            budget.getCategory(),
                            BudgetAlert.AlertType.WEEKLY_SUMMARY
                        );

                        if (alert != null) {
                            alertsCreated++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to create weekly alert for user {} category {}: {}",
                            user.getId(), budget.getCategory(), e.getMessage());
                    }
                }
            }

            log.info("Weekly budget summary completed. Created {} alerts", alertsCreated);
        } catch (Exception e) {
            log.error("Error in weekly budget summary task", e);
        }
    }

    /**
     * Monthly budget summary - runs on 1st day of every month at 9:00 AM
     * Checks all active monthly budgets and creates summary alerts
     */
    @Scheduled(cron = "0 0 9 1 * *")
    public void generateMonthlySummaries() {
        log.info("Starting monthly budget summary task");

        try {
            List<User> users = userRepository.findAll();
            int alertsCreated = 0;

            for (User user : users) {
                List<Budget> monthlyBudgets = budgetRepository.findByUserIdAndIsActive(
                    user.getId(), true
                ).stream()
                .filter(b -> b.getPeriodType() == Budget.PeriodType.MONTHLY)
                .toList();

                for (Budget budget : monthlyBudgets) {
                    try {
                        BudgetAlert alert = budgetAlertService.checkAndCreateAlert(
                            user.getId(),
                            budget.getCategory(),
                            BudgetAlert.AlertType.MONTHLY_SUMMARY
                        );

                        if (alert != null) {
                            alertsCreated++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to create monthly alert for user {} category {}: {}",
                            user.getId(), budget.getCategory(), e.getMessage());
                    }
                }
            }

            log.info("Monthly budget summary completed. Created {} alerts", alertsCreated);
        } catch (Exception e) {
            log.error("Error in monthly budget summary task", e);
        }
    }

    /**
     * Daily budget check - runs every day at 6:00 PM
     * Checks budgets for users who have exceeded their limits
     */
    @Scheduled(cron = "0 0 18 * * *")
    public void dailyBudgetCheck() {
        log.info("Starting daily budget check task");

        try {
            List<User> users = userRepository.findAll();
            int alertsCreated = 0;

            for (User user : users) {
                List<Budget> activeBudgets = budgetRepository.findActiveByUserId(user.getId());

                for (Budget budget : activeBudgets) {
                    try {
                        // Only create alerts for exceeded budgets (>100%)
                        BudgetAlert alert = budgetAlertService.checkAndCreateAlert(
                            user.getId(),
                            budget.getCategory(),
                            BudgetAlert.AlertType.REAL_TIME
                        );

                        if (alert != null && alert.getSeverity() == BudgetAlert.Severity.CRITICAL) {
                            alertsCreated++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to check budget for user {} category {}: {}",
                            user.getId(), budget.getCategory(), e.getMessage());
                    }
                }
            }

            log.info("Daily budget check completed. Created {} critical alerts", alertsCreated);
        } catch (Exception e) {
            log.error("Error in daily budget check task", e);
        }
    }
}
