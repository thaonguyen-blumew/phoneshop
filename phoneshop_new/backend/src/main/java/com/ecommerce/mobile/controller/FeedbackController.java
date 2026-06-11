package com.ecommerce.mobile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecommerce.mobile.entity.Feedback;
import com.ecommerce.mobile.dto.response.FeedbackDto;
import com.ecommerce.mobile.service.FeedbackService;

@Controller
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/support")
    public String customerSupportPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal != null) {
            model.addAttribute("feedbacks", feedbackService.getFeedbacksForCustomer(principal.getUsername())
                    .stream().map(this::toDto).toList());
        }
        return "customer-support";
    }

    @PostMapping("/support/send")
    public String customerCreate(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String content,
                                 RedirectAttributes rt) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            feedbackService.createFeedback(principal.getUsername(), content);
            rt.addFlashAttribute("success", "Đã gửi phản hồi thành công! Chúng tôi sẽ sớm liên hệ lại.");
        } catch (Exception e) {
            rt.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/support";
    }

    @GetMapping("/admin/feedbacks")
    public String employeeList(@AuthenticationPrincipal UserDetails principal, 
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(required = false) String search,
                               Model model) {
        
        java.time.LocalDateTime start = null;
        java.time.LocalDateTime end = null;
        if (startDate != null && !startDate.isEmpty()) {
            start = java.time.LocalDate.parse(startDate).atStartOfDay();
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
        }

        com.ecommerce.mobile.enums.FeedbackStatus feedbackStatus = null;
        if (status != null && !status.isEmpty()) {
            try { feedbackStatus = com.ecommerce.mobile.enums.FeedbackStatus.valueOf(status); } catch (Exception e) {}
        }

        model.addAttribute("pendingFeedbacks", feedbackService.getFilteredFeedbacks(com.ecommerce.mobile.enums.FeedbackStatus.PENDING, start, end, search).stream().map(this::toDto).toList());
        model.addAttribute("myFeedbacks", feedbackService.getFilteredEmployeeFeedbacks(principal.getUsername(), feedbackStatus, start, end, search).stream().map(this::toDto).toList());
        
        model.addAttribute("currentFilter", status != null ? status : "");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);
        return "admin/feedbacks";
    }

    @PostMapping("/admin/feedbacks/{feedbackId}/assign")
    public String assignToMe(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long feedbackId,
                             RedirectAttributes rt) {
        try {
            feedbackService.assignToMe(principal.getUsername(), feedbackId);
            rt.addFlashAttribute("success", "Đã nhận xử lý phản hồi");
        } catch (Exception e) {
            rt.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/feedbacks";
    }

    @PostMapping("/admin/feedbacks/{feedbackId}/resolve")
    public String resolve(@AuthenticationPrincipal UserDetails principal,
                          @PathVariable Long feedbackId,
                          @RequestParam String resolution,
                          RedirectAttributes rt) {
        try {
            feedbackService.resolveFeedback(principal.getUsername(), feedbackId, resolution);
            rt.addFlashAttribute("success", "Đã xử lý phản hồi");
        } catch (Exception e) {
            rt.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/feedbacks";
    }

    private FeedbackDto toDto(Feedback feedback) {
        if (feedback == null) {
            return null;
        }
        return FeedbackDto.builder()
                .feedbackId(feedback.getFeedbackId())
                .content(feedback.getContent())
                .resolution(feedback.getResolution())
                .status(feedback.getStatus() != null ? feedback.getStatus().name() : null)
                .createdAt(feedback.getCreatedAt())
                .resolvedAt(feedback.getResolvedAt())
                .customerName(feedback.getCustomer() != null ? feedback.getCustomer().getFullName() : null)
                .employeeName(feedback.getEmployee() != null ? feedback.getEmployee().getFullName() : null)
                .build();
    }
}
