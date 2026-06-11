package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.service.ProductService;
import com.ecommerce.mobile.service.ReviewService;
import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.dto.response.ProductDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

@RestController
@CrossOrigin("*")
public class ProductController {

    private static final int DEFAULT_PAGE_SIZE = 8;

    @Autowired
    private ProductService productService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/api/products")
    public ApiResponse<Map<String, Object>> listProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

        Page<Product> products;
        if ((brand == null || brand.isBlank())
                && minPrice == null
                && maxPrice == null
                && (sort == null || sort.isBlank())
                && size == DEFAULT_PAGE_SIZE) {
            products = productService.getActiveProducts(keyword, page, DEFAULT_PAGE_SIZE);
        } else {
            products = productService.getActiveProducts(keyword, brand, minPrice, maxPrice, sort, page, size);
        }
        Page<ProductDto> dtos = products.map(com.ecommerce.mobile.mapper.ProductMapper::toDto);
        Map<String, Object> data = new HashMap<>();
        data.put("content", dtos.getContent());
        data.put("number", dtos.getNumber());
        data.put("size", dtos.getSize());
        data.put("totalPages", dtos.getTotalPages());
        data.put("totalElements", dtos.getTotalElements());
        data.put("first", dtos.isFirst());
        data.put("last", dtos.isLast());
        return ApiResponse.success("Lấy danh sách sản phẩm thành công", data);
    }

    @GetMapping("/api/products/{id}")
    public ApiResponse<Map<String, Object>> productDetail(@PathVariable Long id,
                                Principal principal) {

        Product product = productService.getActiveProductDetailById(id);

        if (product == null) {
            return ApiResponse.error(404, "Không tìm thấy sản phẩm");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("product", com.ecommerce.mobile.mapper.ProductMapper.toDto(product));
        data.put("reviews", reviewService.getReviewsForProduct(id).stream()
                .map(reviewService::toDto)
                .toList());
        data.put("averageRating", reviewService.getAverageRating(id));
        data.put("reviewCount", reviewService.getReviewCount(id));
        if (principal != null) {
            data.put("myReview", reviewService.toDto(reviewService.getMyReview(principal.getName(), id)));
            data.put("canReview", reviewService.canReviewProduct(principal.getName(), id));
        } else {
            data.put("myReview", null);
            data.put("canReview", false);
        }
        
        return ApiResponse.success("Lấy chi tiết sản phẩm thành công", data);
    }
}
