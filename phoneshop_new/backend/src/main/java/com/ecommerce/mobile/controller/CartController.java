package com.ecommerce.mobile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecommerce.mobile.entity.Cart;
import com.ecommerce.mobile.service.CartService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String viewCart(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        Cart cart = cartService.getCartByCustomerEmail(principal.getUsername());
        model.addAttribute("cart", cart);
        model.addAttribute("total", cartService.calculateTotal(cart));
        return "cart";
    }

    @PostMapping("/add/{variantId}")
    public String addToCart(@AuthenticationPrincipal UserDetails principal,
                            @PathVariable Long variantId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            cartService.addToCart(principal.getUsername(), variantId, quantity);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/update/{itemId}")
    public String updateItem(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long itemId,
                             @RequestParam Integer quantity,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        if (principal != null) {
            try {
                cartService.updateItemQuantity(principal.getUsername(), itemId, quantity);
            } catch (RuntimeException e) {
                ra.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{itemId}")
    public String removeItem(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long itemId) {
        if (principal != null) {
            cartService.removeItem(principal.getUsername(), itemId);
        }
        return "redirect:/cart";
    }
}
