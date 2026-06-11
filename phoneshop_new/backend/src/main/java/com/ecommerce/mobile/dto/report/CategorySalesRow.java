package com.ecommerce.mobile.dto.report;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CategorySalesRow {
    private Long categoryId;
    private String categoryName;
    private long quantitySold;
    private BigDecimal grossRevenue = BigDecimal.ZERO;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private BigDecimal estimatedProfit = BigDecimal.ZERO;
    private BigDecimal grossProfitMargin = BigDecimal.ZERO;
}
