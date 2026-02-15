package com.receiptscan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for transaction data with computed fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;

    private Long userId;

    private String imageUrl; // Nullable for manual entries

    private String merchantName;

    private BigDecimal amount;

    private LocalDate transactionDate;

    private String category;

    private String paymentMethod;

    private String notes;

    private Boolean isRecurring;

    private Long recurringExpenseId;

    private List<LineItemResponse> items;

    private BigDecimal confidenceScore;

    private boolean isManualEntry; // Computed: imageUrl == null

    private boolean needsReview; // Computed: low confidence, $0 amount, or "Unknown" merchant

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
