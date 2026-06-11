package com.ecommerce.mobile.dto.report;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ManagerReportSummary {
    private long totalOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long activeProducts;
    private long totalVariants;
    private long lowStockVariants;
    private long outOfStockVariants;
    private long totalStockQty;
    private long totalCustomers;
    private long newCustomers;
    private long totalProductsSold;

    private BigDecimal grossRevenue = BigDecimal.ZERO;
    private BigDecimal realizedRevenue = BigDecimal.ZERO;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private BigDecimal estimatedProfit = BigDecimal.ZERO;
    private BigDecimal grossProfitMargin = BigDecimal.ZERO;
    private BigDecimal inventoryValue = BigDecimal.ZERO;
}
