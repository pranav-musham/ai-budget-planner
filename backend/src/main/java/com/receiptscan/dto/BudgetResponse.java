package com.receiptscan.dto;

import com.receiptscan.entity.Budget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private Long id;
    private String category;
    private BigDecimal limitAmount;
    private Budget.LimitType limitType;
    private Budget.PeriodType periodType;
    private Boolean isActive;
    private BigDecimal currentSpending;
    private BigDecimal remainingAmount;
    private BigDecimal percentageUsed;
    private String status; // "SAFE", "WARNING", "EXCEEDED"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
