package com.ecommerce.mobile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = { "items", "items.variant", "items.variant.product", "customer" })
    @Query("select c from Cart c where c.customer.userID = :customerId")
    Optional<Cart> findDetailedByCustomerId(@Param("customerId") Long customerId);

    @EntityGraph(attributePaths = { "items", "items.variant", "items.variant.product", "customer" })
    @Query("select c from Cart c where c.customer.email = :email")
    Optional<Cart> findDetailedByCustomerEmail(@Param("email") String email);
}
