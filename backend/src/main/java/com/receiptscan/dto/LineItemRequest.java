package com.receiptscan.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a line item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(min = 1, max = 200, message = "Item name must be between 1 and 200 characters")
    private String name;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Unit price must have at most 2 decimal places")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price; // Optional, auto-calculated as quantity * unitPrice if not provided
}
