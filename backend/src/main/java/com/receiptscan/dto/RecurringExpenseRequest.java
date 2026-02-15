package com.receiptscan.dto;

import com.receiptscan.entity.RecurringExpense;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpenseRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must be at most 50 characters")
    private String category;

    @NotNull(message = "Frequency is required")
    private RecurringExpense.Frequency frequency;

    @NotNull(message = "Next due date is required")
    private LocalDate nextDueDate;

    @Min(value = 0, message = "Reminder days must be at least 0")
    @Max(value = 30, message = "Reminder days must be at most 30")
    private Integer reminderDays;

    @Size(max = 50, message = "Payment method must be at most 50 characters")
    private String paymentMethod;

    private String notes;
}
