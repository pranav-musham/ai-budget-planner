package com.receiptscan.service;

import com.receiptscan.dto.BudgetRequest;
import com.receiptscan.dto.BudgetResponse;
import com.receiptscan.entity.Budget;
import com.receiptscan.entity.User;
import com.receiptscan.exception.BadRequestException;
import com.receiptscan.exception.NotFoundException;
import com.receiptscan.repository.BudgetRepository;
import com.receiptscan.repository.TransactionRepository;
import com.receiptscan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    @SuppressWarnings("null")
    public BudgetResponse createBudget(Long userId, BudgetRequest request) {
        log.info("Creating budget for user {} in category {}", userId, request.getCategory());

        User user = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        // Check if budget already exists for this category and period
        boolean exists = budgetRepository.existsByUserIdAndCategoryAndPeriodType(
            userId, request.getCategory(), request.getPeriodType()
        );

        if (exists) {
            throw new BadRequestException(
                "Budget already exists for category " + request.getCategory() +
                " with period " + request.getPeriodType()
            );
        }

        Budget budget = Budget.builder()
            .user(user)
            .category(request.getCategory())
            .limitAmount(request.getLimitAmount())
            .limitType(request.getLimitType())
            .periodType(request.getPeriodType())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

        Budget savedBudget = budgetRepository.save(budget);
        log.info("Budget created with ID: {}", savedBudget.getId());
        return toBudgetResponse(savedBudget, userId);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets(Long userId) {
        log.info("Fetching all budgets for user {}", userId);
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        return budgets.stream()
            .map(budget -> toBudgetResponse(budget, userId))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getActiveBudgets(Long userId) {
        log.info("Fetching active budgets for user {}", userId);
        List<Budget> budgets = budgetRepository.findActiveByUserId(userId);
        return budgets.stream()
            .map(budget -> toBudgetResponse(budget, userId))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long userId, Long budgetId) {
        log.info("Fetching budget {} for user {}", budgetId, userId);
        Budget budget = budgetRepository.findById(Objects.requireNonNull(budgetId))
            .orElseThrow(() -> new NotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new BadRequestException("Budget does not belong to user");
        }

        return toBudgetResponse(budget, userId);
    }

    @Transactional
    public BudgetResponse updateBudget(Long userId, Long budgetId, BudgetRequest request) {
        log.info("Updating budget {} for user {}", budgetId, userId);

        Budget budget = budgetRepository.findById(Objects.requireNonNull(budgetId))
            .orElseThrow(() -> new NotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new BadRequestException("Budget does not belong to user");
        }

        budget.setCategory(request.getCategory());
        budget.setLimitAmount(request.getLimitAmount());
        budget.setLimitType(request.getLimitType());
        budget.setPeriodType(request.getPeriodType());
        if (request.getIsActive() != null) {
            budget.setIsActive(request.getIsActive());
        }

        budget = budgetRepository.save(budget);
        log.info("Budget {} updated successfully", budgetId);

        return toBudgetResponse(budget, userId);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        log.info("Deleting budget {} for user {}", budgetId, userId);

        Budget budget = budgetRepository.findById(Objects.requireNonNull(budgetId))
            .orElseThrow(() -> new NotFoundException("Budget not found"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new BadRequestException("Budget does not belong to user");
        }

        budgetRepository.delete(budget);
        log.info("Budget {} deleted successfully", budgetId);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateCategorySpending(Long userId, String category, Budget.PeriodType periodType) {
        LocalDate[] dateRange = getDateRangeForPeriod(periodType);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];

        log.debug("Calculating spending for user {} in category {} from {} to {}",
            userId, category, startDate, endDate);

        return transactionRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate)
            .stream()
            .filter(transaction -> transaction.getCategory().equalsIgnoreCase(category))
            .map(transaction -> transaction.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDate[] getDateRangeForPeriod(Budget.PeriodType periodType) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (periodType) {
            case WEEKLY:
                startDate = endDate.minusWeeks(1);
                break;
            case MONTHLY:
                startDate = endDate.withDayOfMonth(1);
                break;
            case YEARLY:
                startDate = endDate.withDayOfYear(1);
                break;
            default:
                startDate = endDate.withDayOfMonth(1);
        }

        return new LocalDate[]{startDate, endDate};
    }

    private BudgetResponse toBudgetResponse(Budget budget, Long userId) {
        BigDecimal currentSpending = calculateCategorySpending(
            userId,
            budget.getCategory(),
            budget.getPeriodType()
        );

        BigDecimal limitAmount = budget.getLimitAmount();
        BigDecimal remainingAmount = limitAmount.subtract(currentSpending);

        BigDecimal percentageUsed = BigDecimal.ZERO;
        if (limitAmount.compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = currentSpending
                .multiply(BigDecimal.valueOf(100))
                .divide(limitAmount, 2, RoundingMode.HALF_UP);
        }

        String status;
        if (percentageUsed.compareTo(BigDecimal.valueOf(100)) > 0) {
            status = "EXCEEDED";
        } else if (percentageUsed.compareTo(BigDecimal.valueOf(80)) >= 0) {
            status = "WARNING";
        } else {
            status = "SAFE";
        }

        return BudgetResponse.builder()
            .id(budget.getId())
            .category(budget.getCategory())
            .limitAmount(budget.getLimitAmount())
            .limitType(budget.getLimitType())
            .periodType(budget.getPeriodType())
            .isActive(budget.getIsActive())
            .currentSpending(currentSpending)
            .remainingAmount(remainingAmount)
            .percentageUsed(percentageUsed)
            .status(status)
            .createdAt(budget.getCreatedAt())
            .updatedAt(budget.getUpdatedAt())
            .build();
    }
}
