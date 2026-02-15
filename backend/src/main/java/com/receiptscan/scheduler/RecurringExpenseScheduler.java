package com.receiptscan.scheduler;

import com.receiptscan.service.RecurringExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for processing recurring expenses.
 * Runs daily to:
 * 1. Create transactions for due recurring expenses
 * 2. Send reminders for upcoming bills
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseScheduler {

    private final RecurringExpenseService recurringExpenseService;

    /**
     * Process due recurring expenses - runs daily at 1:00 AM
     * Creates transactions for any recurring expenses that are due
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void processDueExpenses() {
        log.info("Starting scheduled job: Process due recurring expenses");
        try {
            int processed = recurringExpenseService.processDueExpenses();
            log.info("Completed processing due expenses. Created {} transactions", processed);
        } catch (Exception e) {
            log.error("Error processing due expenses: {}", e.getMessage(), e);
        }
    }

    /**
     * Send bill reminders - runs daily at 9:00 AM
     * Sends notifications for upcoming bills based on reminder settings
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendBillReminders() {
        log.info("Starting scheduled job: Send bill reminders");
        try {
            int reminders = recurringExpenseService.sendReminders();
            log.info("Completed sending reminders. Sent {} notifications", reminders);
        } catch (Exception e) {
            log.error("Error sending reminders: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing - process due expenses immediately
     */
    public int triggerProcessDueExpenses() {
        log.info("Manual trigger: Process due recurring expenses");
        return recurringExpenseService.processDueExpenses();
    }

    /**
     * Manual trigger for testing - send reminders immediately
     */
    public int triggerSendReminders() {
        log.info("Manual trigger: Send bill reminders");
        return recurringExpenseService.sendReminders();
    }
}
