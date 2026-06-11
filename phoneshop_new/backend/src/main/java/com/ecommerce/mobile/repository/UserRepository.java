package com.ecommerce.mobile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.User;
import java.util.Optional;

@Repository

// JpaRepository<Entity, KiểuKhóaChính>
// Đã có sẵn: findById, findAll, save, delete, count, ...

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Optional<User> findByEmailAndIsActive(String email, Boolean isActive);
    Optional<User> findByResetToken(String resetToken);

}
