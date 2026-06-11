package com.ecommerce.mobile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Modifying
    @Query("delete from ProductImage i where i.variant.product.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("delete from ProductImage i where i.variant.variantId = :variantId")
    void deleteByVariantId(@Param("variantId") Long variantId);
}
