package com.ecommerce.mobile.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.entity.Review;
import com.ecommerce.mobile.dto.response.ReviewDto;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.enums.ProductStatus;
import com.ecommerce.mobile.repository.OrderItemRepository;
import com.ecommerce.mobile.repository.ProductRepository;
import com.ecommerce.mobile.repository.ReviewRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerService customerService;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         OrderItemRepository orderItemRepository,
                         CustomerService customerService,
                         ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerService = customerService;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Review> getReviewsForProduct(Long productId) {
        return reviewRepository.findByProductProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        return reviewRepository.averageRatingByProductId(productId);
    }

    @Transactional(readOnly = true)
    public long getReviewCount(Long productId) {
        return reviewRepository.countByProductProductId(productId);
    }

    @Transactional(readOnly = true)
    public Review getMyReview(String customerEmail, Long productId) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        return reviewRepository.findByCustomerUserIDAndProductProductId(customer.getUserID(), productId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean canReviewProduct(String customerEmail, Long productId) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        return orderItemRepository
                .existsByOrderCustomerUserIDAndVariantProductProductIdAndOrderStatus(
                        customer.getUserID(),
                        productId,
                        OrderStatus.DELIVERED);
    }

    @Transactional
    public Review saveReview(String customerEmail, Long productId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("Số sao phải từ 1 đến 5");
        }

        Product product = productRepository.findDetailedByProductId(productId)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        if (!canReviewProduct(customerEmail, productId)) {
            throw new RuntimeException("Bạn cần mua sản phẩm này trước khi đánh giá");
        }

        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        Review review = reviewRepository.findByCustomerUserIDAndProductProductId(customer.getUserID(), productId)
                .orElseGet(Review::new);

        review.setCustomer(customer);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteMyReview(String customerEmail, Long reviewId) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        Review review = reviewRepository.findByReviewIdAndCustomerUserID(reviewId, customer.getUserID())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
        reviewRepository.delete(review);
    }

    public static String formatAverageRating(Double averageRating) {
        if (averageRating == null) {
            return "Chưa có đánh giá";
        }
        return String.format(Locale.forLanguageTag("vi-VN"), "%.1f/5", averageRating);
    }

    public ReviewDto toDto(Review review) {
        if (review == null) {
            return null;
        }
        return ReviewDto.builder()
                .reviewId(review.getReviewId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .customerName(review.getCustomer() != null ? review.getCustomer().getFullName() : null)
                .build();
    }
}
