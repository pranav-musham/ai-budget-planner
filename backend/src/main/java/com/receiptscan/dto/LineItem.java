package com.receiptscan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineItem {
    private String name;
    private Integer quantity; // Default to 1 if not provided
    private BigDecimal unitPrice;
    private BigDecimal price; // Total price (quantity * unitPrice)
}
