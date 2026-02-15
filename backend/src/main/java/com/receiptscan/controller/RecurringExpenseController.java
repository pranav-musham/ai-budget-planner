package com.receiptscan.controller;

import com.receiptscan.dto.RecurringExpenseRequest;
import com.receiptscan.dto.RecurringExpenseResponse;
import com.receiptscan.entity.User;
import com.receiptscan.service.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring")
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    /**
     * Create a new recurring expense
     * POST /api/recurring
     */
    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody RecurringExpenseRequest request
    ) {
        log.info("Creating recurring expense for user {}: {}", user.getId(), request.getName());
        RecurringExpenseResponse response = recurringExpenseService.createRecurringExpense(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all recurring expenses
     * GET /api/recurring
     */
    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponse>> getAllRecurringExpenses(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        log.info("Fetching recurring expenses for user {} (activeOnly: {})", user.getId(), activeOnly);
        List<RecurringExpenseResponse> expenses = activeOnly
            ? recurringExpenseService.getActiveRecurringExpenses(user.getId())
            : recurringExpenseService.getAllRecurringExpenses(user.getId());
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get upcoming recurring expenses
     * GET /api/recurring/upcoming
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<RecurringExpenseResponse>> getUpcomingExpenses(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false, defaultValue = "30") int days
    ) {
        log.info("Fetching upcoming expenses for user {} within {} days", user.getId(), days);
        return ResponseEntity.ok(recurringExpenseService.getUpcomingExpenses(user.getId(), days));
    }

    /**
     * Get total monthly recurring amount
     * GET /api/recurring/total-monthly
     */
    @GetMapping("/total-monthly")
    public ResponseEntity<Map<String, BigDecimal>> getTotalMonthlyRecurring(
        @AuthenticationPrincipal User user
    ) {
        log.info("Getting total monthly recurring for user {}", user.getId());
        BigDecimal total = recurringExpenseService.getTotalMonthlyRecurring(user.getId());
        return ResponseEntity.ok(Map.of("totalMonthlyRecurring", total));
    }

    /**
     * Get a specific recurring expense
     * GET /api/recurring/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> getRecurringExpense(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        log.info("Fetching recurring expense {} for user {}", id, user.getId());
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpenseById(user.getId(), id));
    }

    /**
     * Update a recurring expense
     * PUT /api/recurring/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> updateRecurringExpense(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @Valid @RequestBody RecurringExpenseRequest request
    ) {
        log.info("Updating recurring expense {} for user {}", id, user.getId());
        return ResponseEntity.ok(recurringExpenseService.updateRecurringExpense(user.getId(), id, request));
    }

    /**
     * Toggle pause status
     * PUT /api/recurring/{id}/pause
     */
    @PutMapping("/{id}/pause")
    public ResponseEntity<RecurringExpenseResponse> togglePause(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        log.info("Toggling pause for recurring expense {} for user {}", id, user.getId());
        return ResponseEntity.ok(recurringExpenseService.togglePause(user.getId(), id));
    }

    /**
     * Delete a recurring expense
     * DELETE /api/recurring/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringExpense(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        log.info("Deleting recurring expense {} for user {}", id, user.getId());
        recurringExpenseService.deleteRecurringExpense(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
