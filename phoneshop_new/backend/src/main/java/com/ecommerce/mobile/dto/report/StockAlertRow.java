package com.ecommerce.mobile.dto.report;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class StockAlertRow {
    private Long variantId;
    private String productName;
    private String sku;
    private Integer storageGb;
    private Integer stockQty;
    private BigDecimal price = BigDecimal.ZERO;
    private BigDecimal importPrice = BigDecimal.ZERO;
    private BigDecimal inventoryValue = BigDecimal.ZERO;
}
