package com.ecommerce.mobile.entity;

import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
    /// category_id,name,slug is_active parent_id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column (name = "name")
    private String name;
    @Column (name = "slug")
    private String slug;
    @Column (name = "is_active")
    private Boolean isActive;
    
    @ManyToOne
    @JoinColumn (name = "parent_id")
        @ToString.Exclude
    @EqualsAndHashCode.Exclude // câu hỏi thằng này có thằng cha là gì?
    private Category parent;

    @OneToMany(mappedBy = "category")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Product> products; // quan hệ 1 nhiều.

    @OneToMany(mappedBy = "parent")
        @ToString.Exclude
    @EqualsAndHashCode.Exclude // thêm code để trả lời câu hỏi thằng cha có thằng con nào
    private List<Category> children;
}
