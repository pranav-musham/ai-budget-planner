package com.receiptscan.controller;

import com.receiptscan.dto.analytics.*;
import com.receiptscan.entity.User;
import com.receiptscan.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/weekly-spending")
    public ResponseEntity<List<WeeklySpendingResponse>> getWeeklySpending(
            @RequestParam(defaultValue = "4") int weeks,
            @AuthenticationPrincipal User user) {
        log.info("Fetching weekly spending for {} weeks, user {}", weeks, user.getId());
        return ResponseEntity.ok(analyticsService.getWeeklySpending(user.getId(), weeks));
    }

    @GetMapping("/monthly-spending")
    public ResponseEntity<List<MonthlySpendingResponse>> getMonthlySpending(
            @RequestParam(defaultValue = "6") int months,
            @AuthenticationPrincipal User user) {
        log.info("Fetching monthly spending for {} months, user {}", months, user.getId());
        return ResponseEntity.ok(analyticsService.getMonthlySpending(user.getId(), months));
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<List<CategoryBreakdownResponse>> getCategoryBreakdown(
            @RequestParam(defaultValue = "MONTHLY") String period,
            @AuthenticationPrincipal User user) {
        log.info("Fetching category breakdown for period: {}, user {}", period, user.getId());
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(user.getId(), period));
    }

    @GetMapping("/top-categories")
    public ResponseEntity<List<CategoryBreakdownResponse>> getTopCategories(
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal User user) {
        log.info("Fetching top {} categories, user {}", limit, user.getId());
        return ResponseEntity.ok(analyticsService.getTopCategories(user.getId(), limit));
    }

    @GetMapping("/bottom-categories")
    public ResponseEntity<List<CategoryBreakdownResponse>> getBottomCategories(
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal User user) {
        log.info("Fetching bottom {} categories, user {}", limit, user.getId());
        return ResponseEntity.ok(analyticsService.getBottomCategories(user.getId(), limit));
    }

    @GetMapping("/spending-trends")
    public ResponseEntity<List<SpendingTrendResponse>> getSpendingTrends(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @AuthenticationPrincipal User user) {
        log.info("Fetching spending trends from {} to {}, user {}", startDate, endDate, user.getId());
        return ResponseEntity.ok(analyticsService.getSpendingTrends(user.getId(), startDate, endDate));
    }
}
