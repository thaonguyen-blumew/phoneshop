package com.ecommerce.mobile.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDto {
    private Long productId;
    private String name;
    private String brand;
    private String description;
    private String status;
    private BigDecimal price; // Derived from lowest variant price
    private Integer totalStock; // Sum of all variant stock quantities
    private Long categoryId;
    private String categoryName;
    private List<ProductImageDto> images;
    private List<ProductVariantDto> variants;
}
