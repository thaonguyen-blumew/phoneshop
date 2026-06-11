package com.ecommerce.mobile.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.OrderItem;
import com.ecommerce.mobile.enums.OrderStatus;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderOrderId(Long orderId);

    boolean existsByOrderCustomerUserIDAndVariantProductProductIdAndOrderStatusNot(
            Long customerId,
            Long productId,
            OrderStatus status);

    boolean existsByOrderCustomerUserIDAndVariantProductProductIdAndOrderStatus(
            Long customerId,
            Long productId,
            OrderStatus status);
}
