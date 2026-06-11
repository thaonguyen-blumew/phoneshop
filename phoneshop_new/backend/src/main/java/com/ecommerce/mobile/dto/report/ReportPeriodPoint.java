package com.ecommerce.mobile.dto.report;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ReportPeriodPoint {
    private String label;
    private long orders;
    private BigDecimal grossRevenue = BigDecimal.ZERO;
    private BigDecimal realizedRevenue = BigDecimal.ZERO;
    private BigDecimal estimatedProfit = BigDecimal.ZERO;
}
