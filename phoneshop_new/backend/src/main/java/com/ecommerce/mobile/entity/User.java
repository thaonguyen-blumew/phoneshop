package com.ecommerce.mobile.entity;

import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data // tự tạo các hàm get set equals.  
@NoArgsConstructor // tạo constructor k tham số bên trong
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // 1 bảng duy nhất
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING) 
// tạo cột dtype thể hiện role. spring tự tạo.


public abstract class User {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "user_id")
    private Long userID;

    @Column (name = "email", nullable = false, unique = true, length =  100)
    private String email;

    @Column (name = "password_hash", nullable = false)
    private String hashPassword;

    @Column (name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column (name = "phone", length = 15 )
    private String phone;

    @Column (name = "is_active")
    private Boolean isActive;

    @Column (name = "created_at")
    private LocalDateTime createdAt;

    // Quên mật khẩu: token UUID (64 ký tự) + thời gian hết hạn
    @Column(name = "reset_token", length = 64)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @ManyToOne(fetch = FetchType.EAGER) // cái này nên sửa thành EAGER. LAZY  = load Role khi nào cần mới load → tiết kiệm
//EAGER = load Role ngay khi load User   → tốn hơn chút
    @JoinColumn (name = "role_id")
    private Role role;

    @PrePersist // annotation này dùng cho hàm,
    // tự chạy trước khi insert vào DB
    // Dùng để tự gán thời gian tạo tài khoản trước khi insert vào DB. (lấy thời gian hiện tại ấy mà)

    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }
}
