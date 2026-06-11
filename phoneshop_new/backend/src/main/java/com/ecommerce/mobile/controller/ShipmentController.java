package com.ecommerce.mobile.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.Shipment;
import com.ecommerce.mobile.entity.ShipmentTrackingEvent;
import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.service.OrderService;
import com.ecommerce.mobile.service.ShipmentService;
import com.ecommerce.mobile.mapper.OrderMapper;

@RestController
@CrossOrigin("*")
public class ShipmentController {

    private final OrderService orderService;
    private final ShipmentService shipmentService;

    public ShipmentController(OrderService orderService, ShipmentService shipmentService) {
        this.orderService = orderService;
        this.shipmentService = shipmentService;
    }

    @GetMapping("/api/orders/{orderId}/tracking")
    public ApiResponse<Map<String, Object>> tracking(@AuthenticationPrincipal UserDetails principal,
                                                     @PathVariable Long orderId) {
        Order order;
        boolean isStaff = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));
        
        if (isStaff) {
            order = orderService.getOrderByIdForStaff(orderId);
        } else {
            order = orderService.getOrderDetailByCustomerEmail(principal.getUsername(), orderId);
        }

        if (order == null) {
            return ApiResponse.error(404, "Không tìm thấy đơn hàng");
        }

        Shipment shipment = shipmentService.getShipmentForOrder(orderId);
        List<ShipmentTrackingEvent> events = shipmentService.getEventsForOrder(orderId);

        Map<String, Object> data = new HashMap<>();
        data.put("order", OrderMapper.toDto(order));
        data.put("shipment", shipment);
        data.put("events", events);
        return ApiResponse.success("Lấy thông tin vận chuyển thành công", data);
    }

    @PostMapping("/api/orders/{orderId}/tracking/refresh")
    public ApiResponse<Map<String, Object>> refresh(@AuthenticationPrincipal UserDetails principal,
                                                    @PathVariable Long orderId) {
        Order order;
        boolean isStaff = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));
        
        if (isStaff) {
            order = orderService.getOrderByIdForStaff(orderId);
        } else {
            order = orderService.getOrderDetailByCustomerEmail(principal.getUsername(), orderId);
        }

        if (order == null) {
            return ApiResponse.error(404, "Không tìm thấy đơn hàng");
        }

        shipmentService.refreshFromGhn(orderId);

        Shipment shipment = shipmentService.getShipmentForOrder(orderId);
        List<ShipmentTrackingEvent> events = shipmentService.getEventsForOrder(orderId);

        Map<String, Object> data = new HashMap<>();
        data.put("order", OrderMapper.toDto(order));
        data.put("shipment", shipment);
        data.put("events", events);
        return ApiResponse.success("Đã đồng bộ trạng thái vận chuyển từ GHN", data);
    }
}
