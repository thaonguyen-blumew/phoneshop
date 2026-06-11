package com.ecommerce.mobile.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ecommerce.mobile.entity.User;
import com.ecommerce.mobile.repository.UserRepository;
import com.ecommerce.mobile.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class CustomUserDetailsService implements UserDetailsService { //debug 1: lỗi chính tả.
    @Autowired
    private UserRepository userRepository; // dùng userRepository để tìm email cho tất cả user.

    @Autowired
    private LoginAttemptService loginAttemptService;

    // chỉ cần dùng đúng hàm này trong userdetailsservice.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
        // Lấy IP người dùng để check brute force
        String clientIp = getClientIp();
        if (loginAttemptService.isBlocked(clientIp)) {
            long minutes = loginAttemptService.minutesUntilUnlock(clientIp);
            throw new LockedException("Tài khoản tạm thời bị khóa do nhập sai quá nhiều lần. Thử lại sau " + minutes + " phút.");
        }

        // Bước 1 tìm user theo email. Nếu k có trả về lỗi
        User user = userRepository.findByEmail(email).
        orElseThrow(
            () -> new UsernameNotFoundException("Không tìm thấy tài khoản" + email) 
        );
         // Bước 2:kiểm tra xem tài khoản có bị khóa không . Có thì trả về lỗi
         if (user.getIsActive() == null || !user.getIsActive()){
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa bởi admin!");
         }

         // SPRING SECURITY BẮT CÓ TIỀN TỐ ROLE_ TROGN TÊN QUYỀN
         // security viết hasRole("CUSTOMER") -> SPRING KIỂM TRA "ROLE_CUSTOMER"
         // Bước 3: lấy quyền theo role nếu trải qua 2 bước kiểm duyệt

         // Chỗ này thực ra chưa hiểu lắm.
         // Phân quyền?
         GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().getNameRole());



        // Trả về danh sách Đóng gói lại mang cho spring security config xem. 
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getHashPassword(), 
            List.of(authority) // Danh sách quyền
        );
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
