package com.receiptscan.dto;

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
public class IncomeSourceRequest {

    @NotBlank(message = "Source name is required")
    @Size(max = 100, message = "Source name must be less than 100 characters")
    private String sourceName;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @Size(max = 50, message = "Payment method must be less than 50 characters")
    private String paymentMethod;

    private String notes;
}
