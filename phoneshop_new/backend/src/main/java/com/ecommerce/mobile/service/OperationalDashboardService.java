package com.ecommerce.mobile.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.mobile.dto.dashboard.ChartPoint;
import com.ecommerce.mobile.dto.dashboard.DrillDownOrderDto;
import com.ecommerce.mobile.dto.dashboard.OperationalDashboardDto;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.OrderItem;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.repository.OrderRepository;

@Service
public class OperationalDashboardService {

    private final OrderRepository orderRepository;

    public OperationalDashboardService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public OperationalDashboardDto getDashboardData() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOf7DaysAgo = today.minusDays(6).atStartOfDay();

        List<Order> ordersLast7Days = orderRepository.filterOrders(null, startOf7DaysAgo, null);

        List<Order> ordersToday = ordersLast7Days.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(startOfToday))
                .collect(Collectors.toList());

        OperationalDashboardDto dto = new OperationalDashboardDto();
        dto.setOrderStatusesToday(buildOrderStatuses(ordersToday));
        dto.setRevenueLast7Days(buildRevenueLast7Days(ordersLast7Days, today));
        dto.setTopProductsThisWeek(buildTopProducts(ordersLast7Days));
        return dto;
    }

    private List<ChartPoint> buildOrderStatuses(List<Order> ordersToday) {
        Map<OrderStatus, Long> counts = ordersToday.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        List<ChartPoint> points = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            points.add(new ChartPoint(status.name(), counts.getOrDefault(status, 0L)));
        }
        return points;
    }

    private List<ChartPoint> buildRevenueLast7Days(List<Order> orders, LocalDate today) {
        Map<LocalDate, BigDecimal> revenueMap = new HashMap<>();
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getCreatedAt() == null)
                continue;
            LocalDate orderDate = order.getCreatedAt().toLocalDate();
            BigDecimal amount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            revenueMap.merge(orderDate, amount, BigDecimal::add);
        }

        List<ChartPoint> points = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            points.add(new ChartPoint(date.format(formatter), revenueMap.getOrDefault(date, BigDecimal.ZERO)));
        }
        return points;
    }

    private List<ChartPoint> buildTopProducts(List<Order> orders) {
        Map<String, Long> productSales = new HashMap<>();
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED)
                continue;
            if (order.getItems() == null)
                continue;

            for (OrderItem item : order.getItems()) {
                if (item.getVariant() == null || item.getVariant().getProduct() == null)
                    continue;
                Product product = item.getVariant().getProduct();
                String name = product.getName() != null ? product.getName() : "Sản phẩm " + product.getProductId();
                long qty = item.getQuantity() != null ? item.getQuantity() : 0;
                productSales.merge(name, qty, Long::sum);
            }
        }

        return productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new ChartPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrillDownOrderDto> getDrillDownOrdersByStatus(String statusName) {
        OrderStatus status = OrderStatus.valueOf(statusName);
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        List<Order> orders = orderRepository.filterOrders(status, startOfToday, null);
        return orders.stream().map(this::toDrillDownDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DrillDownOrderDto> getDrillDownOrdersByProduct(String productName) {
        LocalDateTime startOf7DaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        List<Order> orders = orderRepository.filterOrders(null, startOf7DaysAgo, null);

        return orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .filter(o -> o.getItems() != null && o.getItems().stream()
                        .anyMatch(i -> i.getVariant() != null &&
                                i.getVariant().getProduct() != null &&
                                productName.equals(i.getVariant().getProduct().getName())))
                .map(this::toDrillDownDto)
                .collect(Collectors.toList());
    }

    private DrillDownOrderDto toDrillDownDto(Order order) {
        DrillDownOrderDto dto = new DrillDownOrderDto();
        dto.setOrderId(order.getOrderId());
        dto.setCustomerName(order.getShippingName() != null ? order.getShippingName()
                : (order.getCustomer() != null ? order.getCustomer().getFullName() : "Khách vãng lai"));
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}
