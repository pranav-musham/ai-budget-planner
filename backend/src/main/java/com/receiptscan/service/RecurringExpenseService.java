package com.receiptscan.service;

import com.receiptscan.dto.RecurringExpenseRequest;
import com.receiptscan.dto.RecurringExpenseResponse;
import com.receiptscan.entity.AppNotification;
import com.receiptscan.entity.Transaction;
import com.receiptscan.entity.RecurringExpense;
import com.receiptscan.entity.User;
import com.receiptscan.exception.BadRequestException;
import com.receiptscan.exception.NotFoundException;
import com.receiptscan.repository.AppNotificationRepository;
import com.receiptscan.repository.TransactionRepository;
import com.receiptscan.repository.RecurringExpenseRepository;
import com.receiptscan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AppNotificationRepository notificationRepository;

    @Transactional
    @SuppressWarnings("null")
    public RecurringExpenseResponse createRecurringExpense(Long userId, RecurringExpenseRequest request) {
        log.info("Creating recurring expense for user {}: {}", userId, request.getName());

        User user = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        RecurringExpense recurringExpense = RecurringExpense.builder()
            .user(user)
            .name(request.getName())
            .amount(request.getAmount())
            .category(request.getCategory())
            .frequency(request.getFrequency())
            .nextDueDate(request.getNextDueDate())
            .reminderDays(request.getReminderDays() != null ? request.getReminderDays() : 3)
            .paymentMethod(request.getPaymentMethod())
            .notes(request.getNotes())
            .isActive(true)
            .isPaused(false)
            .build();

        RecurringExpense saved = recurringExpenseRepository.save(recurringExpense);
        log.info("Recurring expense created with ID: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getAllRecurringExpenses(Long userId) {
        log.info("Fetching all recurring expenses for user {}", userId);
        return recurringExpenseRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getActiveRecurringExpenses(Long userId) {
        log.info("Fetching active recurring expenses for user {}", userId);
        return recurringExpenseRepository.findActiveByUserId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getUpcomingExpenses(Long userId, int days) {
        log.info("Fetching upcoming recurring expenses for user {} within {} days", userId, days);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return recurringExpenseRepository.findUpcomingByUserId(userId, startDate, endDate).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalMonthlyRecurring(Long userId) {
        log.info("Calculating total monthly recurring for user {}", userId);
        BigDecimal total = recurringExpenseRepository.calculateTotalMonthlyRecurring(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public RecurringExpenseResponse getRecurringExpenseById(Long userId, Long expenseId) {
        log.info("Fetching recurring expense {} for user {}", expenseId, userId);
        RecurringExpense expense = recurringExpenseRepository.findById(Objects.requireNonNull(expenseId))
            .orElseThrow(() -> new NotFoundException("Recurring expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new BadRequestException("Recurring expense does not belong to user");
        }

        return toResponse(expense);
    }

    @Transactional
    public RecurringExpenseResponse updateRecurringExpense(Long userId, Long expenseId, RecurringExpenseRequest request) {
        log.info("Updating recurring expense {} for user {}", expenseId, userId);

        RecurringExpense expense = recurringExpenseRepository.findById(Objects.requireNonNull(expenseId))
            .orElseThrow(() -> new NotFoundException("Recurring expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new BadRequestException("Recurring expense does not belong to user");
        }

        expense.setName(request.getName());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setFrequency(request.getFrequency());
        expense.setNextDueDate(request.getNextDueDate());
        if (request.getReminderDays() != null) {
            expense.setReminderDays(request.getReminderDays());
        }
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setNotes(request.getNotes());

        RecurringExpense saved = recurringExpenseRepository.save(expense);
        log.info("Recurring expense {} updated successfully", expenseId);
        return toResponse(saved);
    }

    @Transactional
    public RecurringExpenseResponse togglePause(Long userId, Long expenseId) {
        log.info("Toggling pause status for recurring expense {} for user {}", expenseId, userId);

        RecurringExpense expense = recurringExpenseRepository.findById(Objects.requireNonNull(expenseId))
            .orElseThrow(() -> new NotFoundException("Recurring expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new BadRequestException("Recurring expense does not belong to user");
        }

        expense.setIsPaused(!expense.getIsPaused());
        RecurringExpense saved = recurringExpenseRepository.save(expense);
        log.info("Recurring expense {} is now {}", expenseId, saved.getIsPaused() ? "paused" : "active");
        return toResponse(saved);
    }

    @Transactional
    public void deleteRecurringExpense(Long userId, Long expenseId) {
        log.info("Deleting recurring expense {} for user {}", expenseId, userId);

        RecurringExpense expense = recurringExpenseRepository.findById(Objects.requireNonNull(expenseId))
            .orElseThrow(() -> new NotFoundException("Recurring expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new BadRequestException("Recurring expense does not belong to user");
        }

        recurringExpenseRepository.delete(expense);
        log.info("Recurring expense {} deleted successfully", expenseId);
    }

    /**
     * Process due recurring expenses - called by scheduler
     * Creates transactions for expenses that are due
     */
    @Transactional
    public int processDueExpenses() {
        log.info("Processing due recurring expenses");
        List<RecurringExpense> dueExpenses = recurringExpenseRepository.findDueOrOverdue(LocalDate.now());
        int processed = 0;

        for (RecurringExpense expense : dueExpenses) {
            try {
                createTransactionFromRecurring(expense);
                expense.advanceToNextDueDate();
                recurringExpenseRepository.save(expense);

                // Create notification
                createNotification(expense, AppNotification.NotificationType.RECURRING_CREATED,
                    "Transaction Created",
                    String.format("Auto-created transaction for %s: $%.2f", expense.getName(), expense.getAmount()));

                processed++;
                log.info("Processed recurring expense {}: {}", expense.getId(), expense.getName());
            } catch (Exception e) {
                log.error("Failed to process recurring expense {}: {}", expense.getId(), e.getMessage());
            }
        }

        log.info("Processed {} due recurring expenses", processed);
        return processed;
    }

    /**
     * Send reminders for upcoming expenses - called by scheduler
     */
    @Transactional
    public int sendReminders() {
        log.info("Sending reminders for upcoming expenses");
        LocalDate today = LocalDate.now();
        LocalDate maxReminderDate = today.plusDays(7); // Check up to 7 days ahead

        List<RecurringExpense> needingReminder = recurringExpenseRepository.findNeedingReminder(today, maxReminderDate);
        int sent = 0;

        for (RecurringExpense expense : needingReminder) {
            // Only send if within the reminder window
            LocalDate reminderDate = expense.getNextDueDate().minusDays(expense.getReminderDays());
            if (!today.isBefore(reminderDate) && today.isBefore(expense.getNextDueDate())) {
                long daysUntil = ChronoUnit.DAYS.between(today, expense.getNextDueDate());
                createNotification(expense, AppNotification.NotificationType.BILL_REMINDER,
                    "Upcoming Bill",
                    String.format("%s ($%.2f) is due in %d day%s",
                        expense.getName(), expense.getAmount(), daysUntil, daysUntil == 1 ? "" : "s"));
                sent++;
            }
        }

        log.info("Sent {} bill reminders", sent);
        return sent;
    }

    @SuppressWarnings("null")
    private void createTransactionFromRecurring(RecurringExpense expense) {
        Transaction transaction = Transaction.builder()
            .userId(expense.getUser().getId())
            .merchantName(expense.getName())
            .amount(expense.getAmount())
            .transactionDate(expense.getNextDueDate())
            .category(expense.getCategory())
            .notes("Auto-created from recurring expense: " + expense.getName())
            .isRecurring(true)
            .recurringExpenseId(expense.getId())
            .build();

        transactionRepository.save(transaction);
        log.info("Created transaction from recurring expense {}", expense.getId());
    }

    @SuppressWarnings("null")
    private void createNotification(RecurringExpense expense, AppNotification.NotificationType type,
                                    String title, String message) {
        AppNotification notification = AppNotification.builder()
            .user(expense.getUser())
            .type(type)
            .title(title)
            .message(message)
            .relatedEntityType("RECURRING_EXPENSE")
            .relatedEntityId(expense.getId())
            .isRead(false)
            .build();

        notificationRepository.save(notification);
    }

    private RecurringExpenseResponse toResponse(RecurringExpense expense) {
        LocalDate today = LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(today, expense.getNextDueDate());

        return RecurringExpenseResponse.builder()
            .id(expense.getId())
            .name(expense.getName())
            .amount(expense.getAmount())
            .category(expense.getCategory())
            .frequency(expense.getFrequency())
            .nextDueDate(expense.getNextDueDate())
            .lastProcessedDate(expense.getLastProcessedDate())
            .reminderDays(expense.getReminderDays())
            .paymentMethod(expense.getPaymentMethod())
            .notes(expense.getNotes())
            .isActive(expense.getIsActive())
            .isPaused(expense.getIsPaused())
            .daysUntilDue((int) daysUntilDue)
            .isDueOrOverdue(expense.isDueOrOverdue())
            .createdAt(expense.getCreatedAt())
            .updatedAt(expense.getUpdatedAt())
            .build();
    }
}
