package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"customer", "product"})
    List<Review> findByProductProductIdOrderByCreatedAtDesc(Long productId);

    @EntityGraph(attributePaths = {"customer", "product"})
    Optional<Review> findByCustomerUserIDAndProductProductId(Long customerId, Long productId);

    @EntityGraph(attributePaths = {"customer", "product"})
    Optional<Review> findByReviewIdAndCustomerUserID(Long reviewId, Long customerId);

    long countByProductProductId(Long productId);

    @Query("select avg(r.rating) from Review r where r.product.productId = :productId")
    Double averageRatingByProductId(@Param("productId") Long productId);
}
