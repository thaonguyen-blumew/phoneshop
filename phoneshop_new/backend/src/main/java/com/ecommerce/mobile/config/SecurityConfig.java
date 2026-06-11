package com.ecommerce.mobile.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.security.config.Customizer;
import com.ecommerce.mobile.service.LoginAttemptService;

@Configuration
public class SecurityConfig {
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private RoleBasedSuccessHandler roleBasedSuccessHandler;

    @Autowired
    private JsonAuthFailureHandler jsonAuthFailureHandler;

    @Autowired
    private JsonLogoutSuccessHandler jsonLogoutSuccessHandler;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow specific origins (LiveServer on 5500, localhost:3000 for future use)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight requests for 1 hour
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .userDetailsService(customUserDetailsService)
                // ===== PHAN QUYEN URL =====
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // REVIEW: endpoint đánh giá sản phẩm — yêu cầu CUSTOMER
                        .requestMatchers(
                                "/api/products/*/reviews/**",
                                "/api/products/*/reviews")
                        .hasRole("CUSTOMER")

                        // CONG KHAI -- ai cung vao duoc, khong can dang nhap
                        .requestMatchers(
                                "/", // Trang chu API
                                "/products", // Xem danh sach san pham
                                "/products/**", // Xem chi tiet san pham
                                "/api/products/**", // Xem san pham API
                                "/login", // Trang dang nhap
                                "/register", // Trang dang ky
                                "/forgot-password", // Quên mật khẩu
                                "/reset-password", // Đặt lại mật khẩu
                                "/webhook/ghn/**", // GHN webhook
                                "/webhooks/ghn/**",
                                "/api/payments/vnpay/return", // VNPAY return URL (callback công khai)
                                "/api/payments/vnpay/ipn", // VNPAY IPN (server-to-server)
                                "/assets/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/*.html", // Frontend HTML pages
                                "/api/public/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/favicon.ico")
                        .permitAll()

                        // ADMIN (Manager) — quản trị sản phẩm, nhân viên, báo cáo, dashboard
                        .requestMatchers(
                                "/api/admin/dashboard/**",
                                "/api/admin/**",
                                "/admin/products/**",
                                "/admin/employees/**",
                                "/admin/reports/**",
                                "/admin/dashboard")
                        .hasAnyRole("MANAGER", "ADMIN")

                        // STAFF — quản lý đơn hàng
                        .requestMatchers(
                                "/api/staff/**",
                                "/admin/orders/**",
                                "/admin/staff-dashboard")
                        .hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")

                        // EMPLOYEE — xử lý phản hồi
                        .requestMatchers(
                                "/api/employee/**",
                                "/admin/feedbacks/**")
                        .hasAnyRole("EMPLOYEE", "MANAGER", "ADMIN")

                        // PROFILE — ai cũng xem được hồ sơ cá nhân
                        .requestMatchers(
                                "/api/profile/**",
                                "/profile/**")
                        .hasAnyRole("CUSTOMER", "EMPLOYEE", "MANAGER", "ADMIN")

                        // CUSTOMER — giỏ hàng, đơn hàng, thanh toán, hỗ trợ
                        .requestMatchers(
                                "/api/payments/**",
                                "/api/cart/**",
                                "/api/orders/**",
                                "/cart/**",
                                "/orders/**",
                                "/checkout/**",
                                "/support/**")
                        .hasRole("CUSTOMER")

                        // TAT CA CON LAI: phai dang nhap
                        .anyRequest().authenticated())

                // ===== XỬ LÝ LỖI CHO API =====
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                                        org.springframework.http.HttpStatus.UNAUTHORIZED),
                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")))

                // ===== CAU HINH FORM DANG NHAP (SSR) =====
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            // Reset brute force counter khi đăng nhập thành công
                            String ip = request.getHeader("X-Forwarded-For");
                            if (ip == null || ip.isBlank())
                                ip = request.getRemoteAddr();
                            loginAttemptService.loginSucceeded(ip.split(",")[0].trim());
                            roleBasedSuccessHandler.onAuthenticationSuccess(request, response, authentication);
                        })
                        .failureHandler((request, response, exception) -> {
                            // Tăng counter brute force khi đăng nhập sai
                            String ip = request.getHeader("X-Forwarded-For");
                            if (ip == null || ip.isBlank())
                                ip = request.getRemoteAddr();
                            loginAttemptService.loginFailed(ip.split(",")[0].trim());
                            response.sendRedirect("/login?error=true");
                        })
                        .permitAll())

                // ===== CAU HINH DANG XUAT (SSR) =====
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }
}
