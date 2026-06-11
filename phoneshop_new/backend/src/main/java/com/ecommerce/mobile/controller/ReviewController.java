package com.ecommerce.mobile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.service.ReviewService;

@RestController
@CrossOrigin("*")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/products/{productId}/reviews")
    public ApiResponse<Void> saveReview(@AuthenticationPrincipal UserDetails principal,
                                        @PathVariable Long productId,
                                        @RequestParam Integer rating,
                                        @RequestParam(required = false) String comment) {
        reviewService.saveReview(principal.getUsername(), productId, rating, comment);
        return ApiResponse.success("Đã lưu đánh giá của bạn", null);
    }

    @DeleteMapping("/api/products/{productId}/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(@AuthenticationPrincipal UserDetails principal,
                                          @PathVariable Long productId,
                                          @PathVariable Long reviewId) {
        reviewService.deleteMyReview(principal.getUsername(), reviewId);
        return ApiResponse.success("Đã xóa đánh giá", null);
    }
}
