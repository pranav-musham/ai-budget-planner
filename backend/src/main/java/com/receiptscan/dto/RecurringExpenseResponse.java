package com.receiptscan.dto;

import com.receiptscan.entity.RecurringExpense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpenseResponse {
    private Long id;
    private String name;
    private BigDecimal amount;
    private String category;
    private RecurringExpense.Frequency frequency;
    private LocalDate nextDueDate;
    private LocalDate lastProcessedDate;
    private Integer reminderDays;
    private String paymentMethod;
    private String notes;
    private Boolean isActive;
    private Boolean isPaused;
    private Integer daysUntilDue; // Computed field
    private Boolean isDueOrOverdue; // Computed field
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
