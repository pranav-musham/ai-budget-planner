package com.receiptscan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for AI-parsed receipt data
 * Contains all extracted fields from OCR text
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedReceipt {
    private String merchantName;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private LocalDate transactionDate;
    private String category;
    private List<ParsedLineItem> items;
    private BigDecimal confidenceScore;

    // Additional metadata
    private String paymentMethod;
    private String transactionId;
    private String address;
    private String phoneNumber;
}
