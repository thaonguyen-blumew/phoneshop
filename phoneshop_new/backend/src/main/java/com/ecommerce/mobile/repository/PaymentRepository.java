package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"order", "order.items", "order.items.variant", "order.customer"})
    Optional<Payment> findDetailedByPaymentId(Long paymentId);

    @EntityGraph(attributePaths = {"order", "order.items", "order.items.variant", "order.customer", "order.payments"})
    Optional<Payment> findDetailedByTransactionRef(String transactionRef);

    List<Payment> findByOrderOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findFirstByOrderOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findByTransactionRef(String transactionRef);
}
