package com.ecommerce.mobile.mapper;

import com.ecommerce.mobile.dto.response.OrderDto;
import com.ecommerce.mobile.dto.response.OrderItemDto;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.OrderItem;

import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderDto toDto(Order order) {
        if (order == null) return null;
        
        return OrderDto.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod().name())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .voucherCode(order.getAppliedVoucherCode())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .confirmedAt(order.getConfirmedAt())
                .shippingAt(order.getShippingAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .items(order.getItems() != null ? 
                        order.getItems().stream().map(OrderMapper::toItemDto).collect(Collectors.toList()) : null)
                .build();
    }

    public static OrderItemDto toItemDto(OrderItem item) {
        if (item == null) return null;
        
        return OrderItemDto.builder()
                .orderItemId(item.getOrderItemId())
                .productId(item.getVariant() != null && item.getVariant().getProduct() != null ? 
                           item.getVariant().getProduct().getProductId() : null)
                .productName(item.getVariant() != null && item.getVariant().getProduct() != null ? 
                             item.getVariant().getProduct().getName() : null)
                .variantName(item.getVariant() != null && item.getVariant().getStorageGb() != null ? item.getVariant().getStorageGb() + "GB" : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .imageUrl(item.getVariant() != null && item.getVariant().getImages() != null && !item.getVariant().getImages().isEmpty() ? item.getVariant().getImages().get(0).getUrl() : null)
                .build();
    }
}
