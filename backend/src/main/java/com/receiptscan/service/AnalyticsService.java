package com.receiptscan.service;

import com.receiptscan.dto.analytics.*;
import com.receiptscan.entity.Transaction;
import com.receiptscan.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public List<WeeklySpendingResponse> getWeeklySpending(Long userId, int weeks) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(weeks);

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                userId, startDate, endDate);

        Map<String, WeeklySpendingResponse> weeklyMap = new LinkedHashMap<>();

        // Initialize all weeks
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = endDate.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            String weekLabel = String.format("Week %d", weeks - i);

            weeklyMap.put(weekLabel, WeeklySpendingResponse.builder()
                    .week(weekLabel)
                    .amount(BigDecimal.ZERO)
                    .startDate(weekStart.toString())
                    .endDate(weekEnd.toString())
                    .build());
        }

        // Aggregate transactions into weeks
        for (Transaction transaction : transactions) {
            LocalDate txDate = transaction.getTransactionDate();

            for (Map.Entry<String, WeeklySpendingResponse> entry : weeklyMap.entrySet()) {
                WeeklySpendingResponse weekData = entry.getValue();
                LocalDate wStart = LocalDate.parse(weekData.getStartDate());
                LocalDate wEnd = LocalDate.parse(weekData.getEndDate());

                if (!txDate.isBefore(wStart) && !txDate.isAfter(wEnd)) {
                    weekData.setAmount(weekData.getAmount().add(transaction.getAmount()));
                    break;
                }
            }
        }

        return new ArrayList<>(weeklyMap.values());
    }

    public List<MonthlySpendingResponse> getMonthlySpending(Long userId, int months) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                userId, startDate, endDate);

        Map<String, MonthlySpendingResponse> monthlyMap = new LinkedHashMap<>();

        // Initialize all months
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = endDate.minusMonths(i);
            String monthLabel = monthDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            Integer year = monthDate.getYear();

            monthlyMap.put(monthLabel, MonthlySpendingResponse.builder()
                    .month(monthLabel)
                    .amount(BigDecimal.ZERO)
                    .year(year)
                    .build());
        }

        // Aggregate transactions into months
        for (Transaction transaction : transactions) {
            String monthLabel = transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("MMM yyyy"));
            if (monthlyMap.containsKey(monthLabel)) {
                MonthlySpendingResponse monthData = monthlyMap.get(monthLabel);
                monthData.setAmount(monthData.getAmount().add(transaction.getAmount()));
            }
        }

        return new ArrayList<>(monthlyMap.values());
    }

    public List<CategoryBreakdownResponse> getCategoryBreakdown(Long userId, String period) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period.toUpperCase()) {
            case "WEEKLY":
                startDate = endDate.minusWeeks(1);
                break;
            case "YEARLY":
                startDate = endDate.minusYears(1);
                break;
            case "MONTHLY":
            default:
                startDate = endDate.minusMonths(1);
        }

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                userId, startDate, endDate);

        if (transactions.isEmpty()) {
            return Collections.emptyList();
        }

        // Group by category
        Map<String, List<Transaction>> categoryMap = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCategory));

        // Calculate total spending
        BigDecimal totalSpending = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build response
        return categoryMap.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<Transaction> categoryTransactions = entry.getValue();

                    BigDecimal categoryAmount = categoryTransactions.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal percentage = totalSpending.compareTo(BigDecimal.ZERO) > 0
                            ? categoryAmount.divide(totalSpending, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return CategoryBreakdownResponse.builder()
                            .category(category)
                            .amount(categoryAmount)
                            .percentage(percentage)
                            .count((long) categoryTransactions.size())
                            .build();
                })
                .sorted(Comparator.comparing(CategoryBreakdownResponse::getAmount).reversed())
                .collect(Collectors.toList());
    }

    public List<CategoryBreakdownResponse> getTopCategories(Long userId, int limit) {
        List<CategoryBreakdownResponse> breakdown = getCategoryBreakdown(userId, "MONTHLY");
        return breakdown.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<CategoryBreakdownResponse> getBottomCategories(Long userId, int limit) {
        List<CategoryBreakdownResponse> breakdown = getCategoryBreakdown(userId, "MONTHLY");
        return breakdown.stream()
                .sorted(Comparator.comparing(CategoryBreakdownResponse::getAmount))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<SpendingTrendResponse> getSpendingTrends(Long userId, String startDateStr, String endDateStr) {
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                userId, startDate, endDate);

        // Group by date
        Map<LocalDate, BigDecimal> dailySpending = new TreeMap<>();

        // Initialize all dates with zero
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dailySpending.put(currentDate, BigDecimal.ZERO);
            currentDate = currentDate.plusDays(1);
        }

        // Aggregate spending by date
        for (Transaction transaction : transactions) {
            LocalDate txDate = transaction.getTransactionDate();
            dailySpending.merge(txDate, transaction.getAmount(), BigDecimal::add);
        }

        return dailySpending.entrySet().stream()
                .map(entry -> SpendingTrendResponse.builder()
                        .date(entry.getKey().toString())
                        .amount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
