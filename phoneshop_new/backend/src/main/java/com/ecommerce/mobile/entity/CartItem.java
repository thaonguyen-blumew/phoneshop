package com.ecommerce.mobile.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Cart cart;

    @ManyToOne(optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductVariant variant;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.addedAt = LocalDateTime.now();
        recalculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal == null ? BigDecimal.ZERO : subtotal;
    }

    public void recalculateSubtotal() {
        if (unitPrice == null || quantity == null) {
            this.subtotal = BigDecimal.ZERO;
            return;
        }
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
