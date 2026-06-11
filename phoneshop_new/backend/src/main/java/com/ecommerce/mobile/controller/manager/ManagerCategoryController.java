package com.ecommerce.mobile.controller.manager;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecommerce.mobile.dto.manager.ManagerCategoryForm;
import com.ecommerce.mobile.entity.Category;
import com.ecommerce.mobile.repository.CategoryRepository;
import com.ecommerce.mobile.service.ManagerService;

@Controller
@RequestMapping("/admin/categories")
public class ManagerCategoryController {

    private final ManagerService managerService;
    private final CategoryRepository categoryRepository;

    public ManagerCategoryController(ManagerService managerService, CategoryRepository categoryRepository) {
        this.managerService = managerService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", managerService.getAllCategories());
        model.addAttribute("categoryForm", new ManagerCategoryForm());
        model.addAttribute("parentCategories", categoryRepository.findByParentIsNull());
        return "admin/categories";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute ManagerCategoryForm form, RedirectAttributes redirectAttributes) {
        try {
            managerService.saveCategory(form);
            redirectAttributes.addFlashAttribute("message", "Lưu danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle")
    public String toggleCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            managerService.toggleCategoryActive(id);
            redirectAttributes.addFlashAttribute("message", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            managerService.deleteCategorySafely(id);
            redirectAttributes.addFlashAttribute("message", "Xóa danh mục thành công, các sản phẩm đã được chuyển sang Chưa phân loại.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
