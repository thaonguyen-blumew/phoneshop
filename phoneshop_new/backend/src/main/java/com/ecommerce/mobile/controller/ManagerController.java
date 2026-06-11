package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.dto.manager.ManagerProductForm;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.mapper.ProductMapper;
import com.ecommerce.mobile.service.ManagerService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public String list(Model model, 
                       @RequestParam(required = false) String search, 
                       @RequestParam(required = false) String statusFilter,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        
        com.ecommerce.mobile.enums.ProductStatus status = null;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            try { status = com.ecommerce.mobile.enums.ProductStatus.valueOf(statusFilter); } catch (Exception e) {}
        }

        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (startDate != null && !startDate.isEmpty()) {
            start = java.time.LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        List<Product> products = managerService.getFilteredProducts(status, start, end, search);
        model.addAttribute("products", products.stream().map(ProductMapper::toDto).toList());
        model.addAttribute("categories", managerService.getAllCategories());
        model.addAttribute("search", search);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/products";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute ManagerProductForm form, RedirectAttributes redirectAttributes) {
        try {
            managerService.saveProduct(form);
            redirectAttributes.addFlashAttribute("success", "Đã lưu sản phẩm thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi lưu sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam String action, RedirectAttributes redirectAttributes) {
        try {
            if ("delete".equals(action)) {
                managerService.softDeleteProduct(id);
                redirectAttributes.addFlashAttribute("success", "Đã chuyển sản phẩm sang INACTIVE");
            } else {
                managerService.restoreProduct(id);
                redirectAttributes.addFlashAttribute("success", "Đã kích hoạt lại sản phẩm");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi thao tác: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
}
