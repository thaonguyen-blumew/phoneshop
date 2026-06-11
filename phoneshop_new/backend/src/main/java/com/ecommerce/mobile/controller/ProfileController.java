package com.ecommerce.mobile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.service.CustomerService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final CustomerService customerService;

    public ProfileController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String getProfile(@AuthenticationPrincipal UserDetails principal, Model model, @RequestParam(required = false) String tab) {
        if (principal == null) return "redirect:/login";
        if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"))) {
            return "redirect:/admin/dashboard";
        }
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        model.addAttribute("customer", customer);
        model.addAttribute("addresses", customerService.getAddresses(customer.getUserID()));
        model.addAttribute("activeTab", tab != null ? tab : "info");
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        try {
            customerService.updateCustomerInfo(customer.getUserID(), fullName, phone);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật thông tin thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật thông tin: " + e.getMessage());
        }
        return "redirect:/profile?tab=info";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        if (newPassword == null || newPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới không được để trống");
            return "redirect:/profile?tab=password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            return "redirect:/profile?tab=password";
        }
        try {
            Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
            customerService.changePassword(customer.getUserID(), oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đã đổi mật khẩu thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/profile?tab=password";
    }
}
