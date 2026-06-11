package com.ecommerce.mobile.entity;

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
@Table(name ="product_images")
public class ProductImage {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)

    private Long imageId;
    @Column(name = "url")
    private String url;
    @Column(name = "is_primary")
    private Boolean isPrimary;

    @ManyToOne
    @JoinColumn(name = "variant_id")
        @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductVariant variant;



}
