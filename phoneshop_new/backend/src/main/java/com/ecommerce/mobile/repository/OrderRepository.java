package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

       @EntityGraph(attributePaths = { "items", "items.variant", "items.variant.images", "customer", "payments",
                     "shipment", "voucher" })
       Optional<Order> findDetailedByOrderId(@Param("orderId") Long orderId);

       @EntityGraph(attributePaths = { "items", "items.variant", "items.variant.images", "customer", "payments",
                     "shipment", "voucher" })
       List<Order> findByCustomerUserIDOrderByCreatedAtDesc(Long customerId);

       @EntityGraph(attributePaths = { "items", "items.variant", "items.variant.images", "customer", "payments",
                     "shipment", "voucher" })
       List<Order> findAllByOrderByCreatedAtDesc();

       @EntityGraph(attributePaths = { "items", "items.variant", "customer", "payments", "shipment" })
       @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE " +
                     "(:status IS NULL OR o.status = :status) AND " +
                     "(:start IS NULL OR o.createdAt >= :start) AND " +
                     "(:end IS NULL OR o.createdAt <= :end) " +
                     "ORDER BY o.createdAt DESC")
       List<Order> filterOrders(
                     @org.springframework.data.repository.query.Param("status") com.ecommerce.mobile.enums.OrderStatus status,
                     @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
                     @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

       @EntityGraph(attributePaths = { "items", "items.variant", "customer", "payments", "shipment" })
       @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE " +
                     "o.customer.userID = :userId AND " +
                     "(:status IS NULL OR o.status = :status) AND " +
                     "(:start IS NULL OR o.createdAt >= :start) AND " +
                     "(:end IS NULL OR o.createdAt <= :end) " +
                     "ORDER BY o.createdAt DESC")
       List<Order> filterCustomerOrders(@org.springframework.data.repository.query.Param("userId") Long userId,
                     @org.springframework.data.repository.query.Param("status") com.ecommerce.mobile.enums.OrderStatus status,
                     @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
                     @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

       Optional<Order> findByOrderCode(String orderCode);
}
