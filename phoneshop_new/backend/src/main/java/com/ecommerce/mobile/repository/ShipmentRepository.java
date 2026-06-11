package com.ecommerce.mobile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @EntityGraph(attributePaths = {"order", "events"})
    Optional<Shipment> findByOrderOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "events"})
    Optional<Shipment> findByClientOrderCode(String clientOrderCode);

    @EntityGraph(attributePaths = {"order", "events"})
    Optional<Shipment> findByGhnOrderCode(String ghnOrderCode);
}
