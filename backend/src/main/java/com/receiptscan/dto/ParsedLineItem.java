package com.receiptscan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for AI-parsed line item with additional metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedLineItem {
    private String name;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal price;  // Total price for this item
    private String category;   // Item-level category (e.g., "Dairy", "Bakery")
}
