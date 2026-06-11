package com.ecommerce.mobile.mapper;

import com.ecommerce.mobile.dto.response.ProductDto;
import com.ecommerce.mobile.dto.response.ProductImageDto;
import com.ecommerce.mobile.dto.response.ProductVariantDto;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.entity.ProductImage;
import com.ecommerce.mobile.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductMapper {

    public static ProductDto toDto(Product product) {
        if (product == null) return null;

        // Find lowest price
        BigDecimal minPrice = null;
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            for (ProductVariant variant : product.getVariants()) {
                if (variant.getPrice() != null) {
                    if (minPrice == null || variant.getPrice().compareTo(minPrice) < 0) {
                        minPrice = variant.getPrice();
                    }
                }
            }
        }

        // Collect images from all variants
        List<ProductImageDto> imageDtos = new ArrayList<>();
        if (product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                if (variant.getImages() != null) {
                    for (ProductImage image : variant.getImages()) {
                        imageDtos.add(ProductImageDto.builder()
                                .id(image.getImageId())
                                .imageUrl(image.getUrl())
                                .isDefault(image.getIsPrimary())
                                .build());
                    }
                }
            }
        }

        // Calculate total stock
        int totalStock = 0;
        List<ProductVariantDto> variantDtos = new ArrayList<>();
        if (product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                if (variant.getStockQty() != null) {
                    totalStock += variant.getStockQty();
                }
                variantDtos.add(ProductVariantDto.builder()
                        .variantId(variant.getVariantId())
                        .storageGb(variant.getStorageGb())
                        .price(variant.getPrice())
                        .color(variant.getColor())
                        .stockQty(variant.getStockQty())
                        .sku(variant.getSku())
                        .build());
            }
        }

        return ProductDto.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .price(minPrice != null ? minPrice : BigDecimal.ZERO)
                .totalStock(totalStock)
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .images(imageDtos)
                .variants(variantDtos)
                .build();
    }
}
