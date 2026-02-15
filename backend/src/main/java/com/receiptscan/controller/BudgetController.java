package com.receiptscan.controller;

import com.receiptscan.dto.BudgetRequest;
import com.receiptscan.dto.BudgetResponse;
import com.receiptscan.entity.User;
import com.receiptscan.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody BudgetRequest request
    ) {
        try {
            BudgetResponse response = budgetService.createBudget(user.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Error creating budget", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false, defaultValue = "false") boolean activeOnly
    ) {
        try {
            List<BudgetResponse> budgets = activeOnly ?
                budgetService.getActiveBudgets(user.getId()) :
                budgetService.getAllBudgets(user.getId());
            return ResponseEntity.ok(budgets);
        } catch (RuntimeException e) {
            log.error("Error fetching budgets", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudget(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        try {
            BudgetResponse response = budgetService.getBudgetById(user.getId(), id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error fetching budget", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @Valid @RequestBody BudgetRequest request
    ) {
        try {
            BudgetResponse response = budgetService.updateBudget(user.getId(), id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating budget", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        try {
            budgetService.deleteBudget(user.getId(), id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting budget", e);
            return ResponseEntity.notFound().build();
        }
    }
}
