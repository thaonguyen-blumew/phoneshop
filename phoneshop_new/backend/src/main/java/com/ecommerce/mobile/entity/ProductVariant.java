package com.ecommerce.mobile.entity;

import java.math.BigDecimal;
import java.util.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "storage_gb")
    private Integer storageGb;

    @Column(name = "color", length = 50)
    private String color;

    @Column (name = "price")
    private BigDecimal price;

    @Column (name = "import_price")
    private BigDecimal importPrice;

    @Column (name = "stock_qty")
    private Integer stockQty;

    @Column (name = "sku")
    private String sku;

    @ManyToOne
    @JoinColumn(name = "product_id")
        @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @OneToMany(mappedBy = "variant")
        @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductImage> images;


}
