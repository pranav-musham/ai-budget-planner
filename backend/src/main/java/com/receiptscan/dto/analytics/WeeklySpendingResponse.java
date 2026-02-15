package com.receiptscan.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySpendingResponse {
    private String week;
    private BigDecimal amount;
    private String startDate;
    private String endDate;
}
