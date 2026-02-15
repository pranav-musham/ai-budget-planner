package com.receiptscan.controller;

import com.receiptscan.dto.IncomeSourceRequest;
import com.receiptscan.dto.IncomeSourceResponse;
import com.receiptscan.entity.User;
import com.receiptscan.service.IncomeService;
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
@RequestMapping("/api/income")
@RequiredArgsConstructor
@Slf4j
public class IncomeController {

    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<IncomeSourceResponse> createIncomeSource(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody IncomeSourceRequest request
    ) {
        log.info("Creating income entry for user {}: {}", user.getId(), request.getSourceName());
        IncomeSourceResponse response = incomeService.createIncomeSource(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<IncomeSourceResponse>> getAllIncomeSources(
        @AuthenticationPrincipal User user
    ) {
        log.info("Fetching income entries for user {}", user.getId());
        return ResponseEntity.ok(incomeService.getAllIncomeSources(user.getId()));
    }

    @GetMapping("/total-monthly")
    public ResponseEntity<Map<String, BigDecimal>> getTotalMonthlyIncome(
        @AuthenticationPrincipal User user
    ) {
        log.info("Getting total monthly income for user {}", user.getId());
        BigDecimal total = incomeService.getTotalMonthlyIncome(user.getId());
        return ResponseEntity.ok(Map.of("totalMonthlyIncome", total));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncomeSourceResponse> getIncomeSource(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        log.info("Fetching income entry {} for user {}", id, user.getId());
        return ResponseEntity.ok(incomeService.getIncomeSourceById(user.getId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeSourceResponse> updateIncomeSource(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @Valid @RequestBody IncomeSourceRequest request
    ) {
        log.info("Updating income entry {} for user {}", id, user.getId());
        return ResponseEntity.ok(incomeService.updateIncomeSource(user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncomeSource(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        log.info("Deleting income entry {} for user {}", id, user.getId());
        incomeService.deleteIncomeSource(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
