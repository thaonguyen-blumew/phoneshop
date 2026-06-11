package com.ecommerce.mobile.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

@Entity
@Table (name = "address")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @Column (name = "street")
    private String street;

    @Column (name = "ward")
    private String ward;

    @Column (name = "phone")
    private String phone;

    @Column (name = "district")
    private String district;

    @Column (name = "city")
    private String city;

    @Column (name = "is_default")
    private Boolean isDefault;

    @Column (name = "created_at")
    private LocalDateTime createdAt;
    

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Customer customer;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isDefault == null) {
            this.isDefault = false;
        }
    }
    
}
