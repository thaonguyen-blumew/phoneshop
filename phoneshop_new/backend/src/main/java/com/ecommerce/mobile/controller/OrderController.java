package com.ecommerce.mobile.controller;

import java.util.List;
import java.util.stream.Collectors;

import com.ecommerce.mobile.entity.CartItem;

import com.ecommerce.mobile.entity.CartItem;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ecommerce.mobile.entity.Address;
import com.ecommerce.mobile.entity.Cart;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.enums.PaymentMethod;
import com.ecommerce.mobile.service.CartService;
import com.ecommerce.mobile.service.CustomerService;
import com.ecommerce.mobile.service.OrderService;
import com.ecommerce.mobile.service.VnpayService;

import jakarta.servlet.http.HttpServletRequest;

import com.ecommerce.mobile.dto.response.OrderDto;
import com.ecommerce.mobile.mapper.OrderMapper;

@Controller
public class OrderController {

    private final CartService cartService;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final VnpayService vnpayService;

    public OrderController(CartService cartService,
            CustomerService customerService,
            OrderService orderService,
            VnpayService vnpayService) {
        this.cartService = cartService;
        this.customerService = customerService;
        this.orderService = orderService;
        this.vnpayService = vnpayService;
    }

    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long addressId,
            @RequestParam(required = false) List<Long> itemIds,
            Model model) {
        if (principal == null)
            return "redirect:/login";

        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        Cart cart = cartService.getCartByCustomerEmail(principal.getUsername());

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        List<CartItem> displayItems;
        if (itemIds != null && !itemIds.isEmpty()) {
            displayItems = cart.getItems().stream()
                    .filter(i -> itemIds.contains(i.getCartItemId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            displayItems = cart.getItems();
        }

        if (displayItems.isEmpty()) {
            return "redirect:/cart";
        }

        List<Address> addresses = customerService.getAddresses(customer.getUserID());
        Address selectedAddress = null;
        if (addressId != null) {
            selectedAddress = customerService.getAddressForCustomer(customer.getUserID(), addressId);
        } else if (!addresses.isEmpty()) {
            selectedAddress = addresses.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                    .findFirst()
                    .orElse(addresses.get(0));
        }

        java.math.BigDecimal total = displayItems.stream()
                .filter(item -> item.getUnitPrice() != null && item.getQuantity() != null)
                .map(com.ecommerce.mobile.entity.CartItem::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Logic phí ship vùng miền thực tế (đồng bộ với OrderService)
        java.math.BigDecimal shippingFee;
        if (total.compareTo(new java.math.BigDecimal("10000000")) >= 0) {
            shippingFee = java.math.BigDecimal.ZERO;
        } else {
            String city = (selectedAddress != null) ? selectedAddress.getCity() : "";
            if (city != null && (city.contains("Hà Nội") || city.contains("Hồ Chí Minh"))) {
                shippingFee = new java.math.BigDecimal("30000");
            } else {
                shippingFee = new java.math.BigDecimal("50000");
            }
        }

        model.addAttribute("addresses", addresses);
        model.addAttribute("selectedAddress", selectedAddress);
        model.addAttribute("cart", cart);
        model.addAttribute("displayItems", displayItems);
        model.addAttribute("itemIds", itemIds);
        model.addAttribute("total", total);
        model.addAttribute("shippingFee", shippingFee);
        model.addAttribute("finalTotal", total.add(shippingFee));
        model.addAttribute("customer", customer);

        return "checkout";
    }

    @PostMapping("/checkout/place")
    public String placeOrder(@AuthenticationPrincipal UserDetails principal,
            @RequestParam String shippingName,
            @RequestParam String shippingPhone,
            @RequestParam String shippingAddress,
            @RequestParam(required = false) String shippingWard,
            @RequestParam(required = false) String shippingDistrict,
            @RequestParam String shippingCity,
            @RequestParam(required = false) String voucherCode,
            @RequestParam String paymentMethod,
            @RequestParam Long cartId,
            @RequestParam(required = false) List<Long> itemIds,
            HttpServletRequest request) {
        if (principal == null)
            return "redirect:/login";

        PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
        Order order = orderService.placeOrder(
                principal.getUsername(), shippingName, shippingPhone,
                shippingAddress, shippingWard, shippingDistrict,
                shippingCity, voucherCode, method, cartId, itemIds);

        if (method == PaymentMethod.VN_PAY) {
            com.ecommerce.mobile.entity.Payment payment = order.getPayments().stream().findFirst().orElse(null);
            if (payment != null) {
                String paymentUrl = vnpayService.createPaymentUrl(payment, request);
                return "redirect:" + paymentUrl;
            }
        }

        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String orderList(@AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        if (principal == null)
            return "redirect:/login";

        com.ecommerce.mobile.enums.OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = com.ecommerce.mobile.enums.OrderStatus.valueOf(status);
            } catch (Exception e) {
            }
        }

        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (startDate != null && !startDate.isEmpty()) {
            start = java.time.LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        List<Order> orders = orderService.getFilteredOrdersForCustomer(principal.getUsername(), orderStatus, start,
                end);
        List<OrderDto> dtos = orders.stream().map(OrderMapper::toDto).collect(Collectors.toList());

        model.addAttribute("orders", dtos);
        model.addAttribute("currentFilter", status != null ? status : "");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "orders";
    }

    @GetMapping("/orders/{orderId}")
    public String orderDetail(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long orderId, Model model) {
        if (principal == null)
            return "redirect:/login";
        Order order = orderService.getOrderDetailByCustomerEmail(principal.getUsername(), orderId);
        if (order == null) {
            return "redirect:/orders";
        }
        model.addAttribute("order", OrderMapper.toDto(order));
        return "order-detail";
    }

    @PostMapping("/orders/{orderId}/cancel")
    public String cancelOrder(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (principal != null) {
            try {
                orderService.cancelOrderByCustomerEmail(principal.getUsername(), orderId, reason);
                redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng thành công");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            }
        }
        return "redirect:/orders/" + orderId;
    }

    @PostMapping("/orders/{orderId}/complete")
    public String completeOrder(@AuthenticationPrincipal UserDetails principal,
            @PathVariable Long orderId) {
        if (principal != null) {
            orderService.completeOrderByCustomerEmail(principal.getUsername(), orderId);
        }
        return "redirect:/orders/" + orderId;
    }

    @GetMapping("/tracking")
    public String trackingPage(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null)
            return "redirect:/login";
        return "tracking";
    }
}
