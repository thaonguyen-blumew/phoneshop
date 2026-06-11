package com.ecommerce.mobile.entity;

import java.time.LocalDateTime;

import com.ecommerce.mobile.enums.ShipmentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "shipment_events")
@Data
@NoArgsConstructor
public class ShipmentTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", length = 30)
    private ShipmentStatus shipmentStatus;

    @Column(name = "ghn_status", length = 60)
    private String ghnStatus;

    @Column(name = "event_type", length = 60)
    private String eventType;

    @Column(name = "warehouse", length = 255)
    private String warehouse;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @PrePersist
    public void prePersist() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }
}
