package com.ecommerce.mobile.dto.report;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TopProductRow {
    private Long productId;
    private String productName;
    private String brand;
    private long quantitySold;
    private BigDecimal grossRevenue = BigDecimal.ZERO;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private BigDecimal estimatedProfit = BigDecimal.ZERO;
    private BigDecimal grossProfitMargin = BigDecimal.ZERO;
}
