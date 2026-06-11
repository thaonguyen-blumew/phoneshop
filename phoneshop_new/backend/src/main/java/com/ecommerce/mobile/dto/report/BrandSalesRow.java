package com.ecommerce.mobile.dto.report;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BrandSalesRow {
    private String brand;
    private long quantitySold;
    private BigDecimal grossRevenue = BigDecimal.ZERO;
}
