package com.ecommerce.mobile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.service.CustomerService;

@Controller
@RequestMapping("/profile/addresses")
public class AddressController {

    private final CustomerService customerService;

    public AddressController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/add")
    public String add(@AuthenticationPrincipal UserDetails principal,
                      @RequestParam String street,
                      @RequestParam(required = false) String ward,
                      @RequestParam(required = false) String district,
                      @RequestParam String city,
                      @RequestParam(required = false) String phone,
                      @RequestParam(defaultValue = "false") boolean setDefault,
                      RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
            customerService.addAddress(customer.getUserID(), street, ward, district, city, phone, setDefault);
            redirectAttributes.addFlashAttribute("success", "Đã thêm địa chỉ mới");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi thêm địa chỉ: " + e.getMessage());
        }
        return "redirect:/profile?tab=addresses";
    }

    @PostMapping("/{addressId}/edit")
    public String edit(@AuthenticationPrincipal UserDetails principal,
                       @PathVariable Long addressId,
                       @RequestParam String street,
                       @RequestParam(required = false) String ward,
                       @RequestParam(required = false) String district,
                       @RequestParam String city,
                       @RequestParam(required = false) String phone,
                       RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
            customerService.updateAddress(customer.getUserID(), addressId, street, ward, district, city, phone);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật địa chỉ");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật địa chỉ: " + e.getMessage());
        }
        return "redirect:/profile?tab=addresses";
    }

    @PostMapping("/{addressId}/delete")
    public String delete(@AuthenticationPrincipal UserDetails principal,
                         @PathVariable Long addressId,
                         RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
            customerService.deleteAddress(customer.getUserID(), addressId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa địa chỉ");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa địa chỉ: " + e.getMessage());
        }
        return "redirect:/profile?tab=addresses";
    }

    @PostMapping("/{addressId}/default")
    public String setDefault(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long addressId,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
            customerService.setDefaultAddress(customer.getUserID(), addressId);
            redirectAttributes.addFlashAttribute("success", "Đã đặt địa chỉ mặc định");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/profile?tab=addresses";
    }
}
