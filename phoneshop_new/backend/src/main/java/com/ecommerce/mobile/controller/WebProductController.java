package com.ecommerce.mobile.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecommerce.mobile.dto.response.ProductDto;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.mapper.ProductMapper;
import com.ecommerce.mobile.service.ProductService;
import com.ecommerce.mobile.service.ReviewService;

@Controller
public class WebProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    public WebProductController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    @GetMapping("/products")
    public String productsPage(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        
        int size = 12; // 12 products per page on grid
        Page<Product> products;
        
        if ((brand == null || brand.isBlank())
                && minPrice == null
                && maxPrice == null
                && (sort == null || sort.isBlank())) {
            products = productService.getActiveProducts(keyword, page, size);
        } else {
            products = productService.getActiveProducts(keyword, brand, minPrice, maxPrice, sort, page, size);
        }
        
        Page<ProductDto> dtos = products.map(ProductMapper::toDto);
        
        model.addAttribute("productsPage", dtos);
        model.addAttribute("keyword", keyword);
        model.addAttribute("brand", brand);
        model.addAttribute("sort", sort);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        
        return "products";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Principal principal, Model model) {
        Product product = productService.getActiveProductDetailById(id);
        if (product == null) {
            return "redirect:/products";
        }
        
        model.addAttribute("product", ProductMapper.toDto(product));
        model.addAttribute("reviews", reviewService.getReviewsForProduct(id).stream()
                .map(reviewService::toDto).toList());
        model.addAttribute("averageRating", reviewService.getAverageRating(id));
        model.addAttribute("reviewCount", reviewService.getReviewCount(id));
        
        if (principal != null) {
            boolean isCustomer = false;
            if (principal instanceof org.springframework.security.core.Authentication auth) {
                isCustomer = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER") || a.getAuthority().equals("CUSTOMER"));
            }
            if (isCustomer) {
                model.addAttribute("myReview", reviewService.toDto(reviewService.getMyReview(principal.getName(), id)));
                model.addAttribute("canReview", reviewService.canReviewProduct(principal.getName(), id));
            } else {
                model.addAttribute("myReview", null);
                model.addAttribute("canReview", false);
            }
        } else {
            model.addAttribute("myReview", null);
            model.addAttribute("canReview", false);
        }
        
        return "product-detail";
    }
}
