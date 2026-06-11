package com.ecommerce.mobile.controller;

import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecommerce.mobile.service.ManagerEmployeeService;

@Controller
@RequestMapping("/admin/employees")
public class ManagerEmployeeController {

    private final ManagerEmployeeService managerEmployeeService;

    public ManagerEmployeeController(ManagerEmployeeService managerEmployeeService) {
        this.managerEmployeeService = managerEmployeeService;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) Boolean activeFilter,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        
        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (startDate != null && !startDate.isEmpty()) {
            start = java.time.LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        model.addAttribute("employees", managerEmployeeService.getFilteredEmployees(activeFilter, start, end, search));
        model.addAttribute("search", search);
        model.addAttribute("activeFilter", activeFilter);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/employees";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long employeeId,
                       @RequestParam String email,
                       @RequestParam(required = false) String password,
                       @RequestParam String fullName,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) BigDecimal salary,
                       @RequestParam(required = false, defaultValue = "true") Boolean active,
                       RedirectAttributes redirectAttributes) {
        try {
            if (employeeId == null) {
                managerEmployeeService.createEmployee(email, password, fullName, phone, salary, active);
                redirectAttributes.addFlashAttribute("success", "Đã tạo nhân viên mới");
            } else {
                managerEmployeeService.updateEmployee(employeeId, email, fullName, phone, salary, active, password);
                redirectAttributes.addFlashAttribute("success", "Đã cập nhật nhân viên");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirectAttributes) {
        try {
            managerEmployeeService.setActive(id, active);
            redirectAttributes.addFlashAttribute("success", active ? "Đã kích hoạt nhân viên" : "Đã khóa nhân viên");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/employees";
    }
}
