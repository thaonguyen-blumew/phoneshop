package com.ecommerce.mobile.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductVariantDto {
    private Long variantId;
    private Integer storageGb;
    private BigDecimal price;
    private String color;
    private Integer stockQty;
    private String sku;
}
