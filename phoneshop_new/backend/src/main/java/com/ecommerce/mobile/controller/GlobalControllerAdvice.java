package com.ecommerce.mobile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ecommerce.mobile.entity.Cart;
import com.ecommerce.mobile.service.CartService;

@ControllerAdvice(basePackages = "com.ecommerce.mobile.controller")
public class GlobalControllerAdvice {

    private final CartService cartService;
    private final com.ecommerce.mobile.service.CustomerService customerService;

    public GlobalControllerAdvice(CartService cartService, com.ecommerce.mobile.service.CustomerService customerService) {
        this.cartService = cartService;
        this.customerService = customerService;
    }

    @ModelAttribute("customer")
    public com.ecommerce.mobile.entity.Customer populateCustomer(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return null;
        try {
            return customerService.requireCustomerByEmail(principal.getUsername());
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("cartCount")
    public int populateCartCount(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return 0;
        }
        try {
            Cart cart = cartService.getCartByCustomerEmail(principal.getUsername());
            if (cart != null && cart.getItems() != null) {
                return cart.getItems().stream().mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0).sum();
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }
}
