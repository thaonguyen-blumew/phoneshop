package com.ecommerce.mobile.entity;


import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "role_id")
    private Long roleId;

    @Column (name = "name_role", nullable = false)
    private String nameRole;

    @OneToMany(mappedBy = "role") // one to many là trỏ vào object class, còn many to one trỏ vào cột db.
    private List<User> user; // quan hệ 1 nhiều.
  

    

}
