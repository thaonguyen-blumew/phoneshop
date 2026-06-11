package com.ecommerce.mobile.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.mobile.entity.Role;
import java.util.Optional;


public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNameRole(String nameRole);

}
