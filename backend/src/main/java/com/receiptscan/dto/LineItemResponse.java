package com.receiptscan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for line item with full details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineItemResponse {

    private String name;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal price; // Total price (quantity * unitPrice)
}
