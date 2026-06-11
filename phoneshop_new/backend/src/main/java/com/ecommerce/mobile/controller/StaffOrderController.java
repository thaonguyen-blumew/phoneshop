package com.ecommerce.mobile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.service.OrderService;
import com.ecommerce.mobile.service.ShipmentService;

@Controller
@RequestMapping("/admin/orders")
public class StaffOrderController {

    private final OrderService orderService;
    private final ShipmentService shipmentService;

    public StaffOrderController(OrderService orderService, ShipmentService shipmentService) {
        this.orderService = orderService;
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public String list(Model model, 
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        
        com.ecommerce.mobile.enums.OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try { orderStatus = com.ecommerce.mobile.enums.OrderStatus.valueOf(status); } catch (Exception e) { }
        }

        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (startDate != null && !startDate.isEmpty()) {
            start = java.time.LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        model.addAttribute("orders", orderService.getFilteredOrdersForStaff(orderStatus, start, end));
        model.addAttribute("currentFilter", status != null ? status : "");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/orders";
    }

    @PostMapping("/{orderId}/status")
    public String updateStatus(@PathVariable Long orderId,
                               @RequestParam OrderStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.advanceOrderStatusForStaff(orderId, status);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái đơn hàng");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{orderId}/receive")
    public String receiveOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            orderService.receiveOrderForStaff(orderId);
            redirectAttributes.addFlashAttribute("success", "Đã tiếp nhận đơn hàng");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{orderId}/mock-ghn")
    public String mockGhn(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            com.ecommerce.mobile.entity.Order order = orderService.getOrderByIdForStaff(orderId);
            if (order == null || order.getOrderCode() == null) {
                throw new RuntimeException("Không tìm thấy đơn hàng hoặc mã vận đơn (chưa xác nhận?)");
            }
            
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("OrderCode", order.getOrderCode());
            
            String nextStatus = "picking";
            if ("SHIPPING".equals(order.getStatus().name())) {
                nextStatus = "delivered";
            } else if ("CONFIRMED".equals(order.getStatus().name())) {
                nextStatus = "delivering";
            }
            
            payload.put("Status", nextStatus);
            java.util.Map<String, Object> fee = new java.util.HashMap<>();
            fee.put("Total", 30000);
            payload.put("Fee", fee);
            payload.put("ConvertedWeight", 500);
            payload.put("Time", java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()));
            
            shipmentService.processGhnWebhook(payload);
            redirectAttributes.addFlashAttribute("success", "Đã giả lập GHN Webhook thành công (Trạng thái đẩy về: " + nextStatus + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi giả lập: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}
