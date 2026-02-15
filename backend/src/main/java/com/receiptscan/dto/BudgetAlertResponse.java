package com.receiptscan.dto;

import com.receiptscan.entity.BudgetAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAlertResponse {

    private Long id;
    private Long budgetId;
    private BudgetAlert.AlertType alertType;
    private BudgetAlert.Severity severity;
    private String category;
    private BigDecimal currentSpending;
    private BigDecimal budgetLimit;
    private BigDecimal percentageUsed;
    private String message;
    private List<BudgetAlert.AISuggestion> aiSuggestions;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
