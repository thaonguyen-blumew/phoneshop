package com.ecommerce.mobile.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.mobile.entity.User;
import com.ecommerce.mobile.repository.UserRepository;

/**
 * Xử lý luồng Quên Mật Khẩu.
 *
 * Luồng:
 * 1. User vào /forgot-password → nhập email
 * 2. POST /forgot-password → tạo token, lưu vào User, hiển thị link reset ngay trên màn hình (demo mode)
 * 3. User click link → GET /reset-password?token=xxx → nhập mật khẩu mới
 * 4. POST /reset-password → kiểm tra token, đổi mật khẩu, xóa token → redirect /login
 *
 * Note: Trong demo, link reset hiện ngay trên màn hình thay vì gửi email.
 * Khi có Gmail credentials thật → thêm MailService để gửi email.
 */
@Controller
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                         Model model) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        if (user == null) {
            // Không tiết lộ email có tồn tại hay không (bảo mật)
            model.addAttribute("sent", true);
            model.addAttribute("demoToken", null);
            return "forgot-password";
        }

        // Tạo token ngẫu nhiên, hết hạn sau 1 giờ
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Demo mode: hiện link trực tiếp trên trang thay vì gửi email
        model.addAttribute("sent", true);
        model.addAttribute("demoToken", token);
        model.addAttribute("demoEmail", email);
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token,
                                     Model model) {
        if (token == null || token.isBlank()) {
            return "redirect:/forgot-password";
        }

        User user = userRepository.findByResetToken(token).orElse(null);

        if (user == null) {
            model.addAttribute("error", "Link đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng.");
            return "reset-password";
        }

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Link đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại.");
            return "reset-password";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                        @RequestParam String password,
                                        @RequestParam String confirmPassword,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        model.addAttribute("token", token);

        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải từ 6 ký tự trở lên.");
            return "reset-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "reset-password";
        }

        User user = userRepository.findByResetToken(token).orElse(null);

        if (user == null) {
            model.addAttribute("error", "Link đặt lại mật khẩu không hợp lệ.");
            return "reset-password";
        }

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Link đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại.");
            return "reset-password";
        }

        // Đặt mật khẩu mới, xóa token
        user.setHashPassword(passwordEncoder.encode(password));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
        return "redirect:/login?registered=true";
    }
}
