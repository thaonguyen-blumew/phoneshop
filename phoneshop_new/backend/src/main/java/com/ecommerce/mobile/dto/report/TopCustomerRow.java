package com.ecommerce.mobile.dto.report;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TopCustomerRow {
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private long totalOrders;
    private BigDecimal totalSpent = BigDecimal.ZERO;
}
