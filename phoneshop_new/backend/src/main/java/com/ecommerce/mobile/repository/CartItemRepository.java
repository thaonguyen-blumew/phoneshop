package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select ci from CartItem ci where ci.cart.cartId = :cartId")
    List<CartItem> findAllByCartId(@Param("cartId") Long cartId);

    @Query("""
            select ci from CartItem ci
            where ci.cart.cartId = :cartId
              and ci.variant.variantId = :variantId
            """)
    Optional<CartItem> findByCartIdAndVariantId(@Param("cartId") Long cartId,
                                                @Param("variantId") Long variantId);

    @Modifying
    @Query("delete from CartItem ci where ci.cart.cartId = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
}
