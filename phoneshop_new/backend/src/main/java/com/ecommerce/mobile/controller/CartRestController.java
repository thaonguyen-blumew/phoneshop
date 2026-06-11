package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.entity.Cart;
import com.ecommerce.mobile.entity.CartItem;
import com.ecommerce.mobile.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartRestController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add/{variantId}")
    public ResponseEntity<?> addToCart(@AuthenticationPrincipal UserDetails principal,
                                      @PathVariable Long variantId,
                                      @RequestParam(defaultValue = "1") Integer quantity) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để thêm vào giỏ hàng");
        }

        try {
            CartItem item = cartService.addToCart(principal.getUsername(), variantId, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("itemId", item.getCartItemId());
            response.put("cartCount", item.getCart().getItems().size());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}
