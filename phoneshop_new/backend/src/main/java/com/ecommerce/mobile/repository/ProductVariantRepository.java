package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
///implement by codex
/// // tìm kiếm sản phẩm bằng productId (phải có query.)
 //  tìm bằng product Id
 @Query ("""
         select v from ProductVariant v where v.product.productId = :productId
         """)
    List<ProductVariant> findByProductId(@Param ("productId") Long productId);
// me implement.
    Optional<ProductVariant> findBySku(String sku);// tìm bằng sku

    List<ProductVariant> findByStockQtyLessThanEqual(Integer stockQty); // tìm sản phẩm mà tồn kho <  yêu cầu

    @Modifying
    @Query("delete from ProductVariant v where v.product.productId = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
