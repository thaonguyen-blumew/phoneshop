package com.ecommerce.mobile.dto.manager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ManagerProductVariantForm {
    private Long variantId;
    private Integer storageGb;
    private String color;
    private BigDecimal price;
    private BigDecimal importPrice;
    private Integer stockQty;
    private String sku;
    private List<ManagerProductImageForm> images = new ArrayList<>();
}
