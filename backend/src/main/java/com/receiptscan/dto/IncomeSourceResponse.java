package com.receiptscan.dto;

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
public class IncomeSourceResponse {
    private Long id;
    private String sourceName;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
}
