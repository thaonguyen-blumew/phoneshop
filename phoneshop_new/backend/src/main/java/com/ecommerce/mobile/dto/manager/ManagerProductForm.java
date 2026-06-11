package com.ecommerce.mobile.dto.manager;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.mobile.enums.ProductStatus;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ManagerProductForm {
    private Long productId;
    private Long categoryId;
    private String name;
    private String brand;
    private String description;
    private ProductStatus status;
    private List<ManagerProductVariantForm> variants = new ArrayList<>();
}
