package com.ecommerce.mobile.controller;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ecommerce.mobile.entity.Feedback;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.ProductVariant;
import com.ecommerce.mobile.enums.FeedbackStatus;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.service.FeedbackService;
import com.ecommerce.mobile.service.OrderService;
import com.ecommerce.mobile.service.ReportService;
import com.ecommerce.mobile.repository.ProductVariantRepository;

import com.ecommerce.mobile.repository.CustomerRepository;
import com.ecommerce.mobile.entity.Customer;

@Controller
@RequestMapping("/admin")
public class ReportController {

    private final ReportService reportService;
    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;

    public ReportController(ReportService reportService,
                            OrderService orderService,
                            FeedbackService feedbackService,
                            ProductVariantRepository productVariantRepository,
                            CustomerRepository customerRepository) {
        this.reportService = reportService;
        this.orderService = orderService;
        this.feedbackService = feedbackService;
        this.productVariantRepository = productVariantRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        populateDashboardModel(model);
        return "admin/dashboard";
    }

    private void populateDashboardModel(Model model) {
        var report = reportService.getManagerReport("day");
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        
        List<Order> orders = orderService.getAllOrdersForStaff(); // This could be heavy but we'll use it
        List<Order> todayOrders = orders.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(startOfDay))
                .toList();
                
        List<Feedback> pendingFeedbacks = feedbackService.getPendingFeedbacks();
        List<ProductVariant> lowStockVariants = productVariantRepository.findByStockQtyLessThanEqual(5).stream()
                .sorted(Comparator.comparing((ProductVariant v) -> v.getStockQty() == null ? Integer.MAX_VALUE : v.getStockQty())
                        .thenComparing(v -> v.getSku() == null ? "" : v.getSku()))
                .limit(8)
                .toList();

        model.addAttribute("report", report);
        
        // Orders today
        model.addAttribute("pendingOrdersCount", countOrders(todayOrders, OrderStatus.PENDING));
        model.addAttribute("processingOrdersCount", countOrders(todayOrders, OrderStatus.CONFIRMED) + countOrders(todayOrders, OrderStatus.PACKING));
        model.addAttribute("shippingOrdersCount", countOrders(todayOrders, OrderStatus.SHIPPING));
        model.addAttribute("deliveredOrdersCount", countOrders(todayOrders, OrderStatus.DELIVERED));
        model.addAttribute("cancelledOrdersCount", countOrders(todayOrders, OrderStatus.CANCELLED));
        
        // For Notifications section (timeline)
        model.addAttribute("recentOrders", orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(10)
                .toList());
                
        model.addAttribute("recentCustomers", customerRepository.findAll().stream()
                .filter(c -> c.getCreatedAt() != null)
                .sorted(Comparator.comparing(Customer::getCreatedAt).reversed())
                .limit(10)
                .toList());
                
        model.addAttribute("recentPaidOrders", orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED && o.getCreatedAt() != null)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(10)
                .toList());

        model.addAttribute("outOfStockVariantList", lowStockVariants.stream()
                .filter(v -> v != null && v.getStockQty() != null && v.getStockQty() == 0)
                .toList());
        model.addAttribute("lowStockVariantList", lowStockVariants.stream()
                .filter(v -> v != null && v.getStockQty() != null && v.getStockQty() > 0)
                .toList());
    }

    @GetMapping("/staff-dashboard")
    public String staffDashboard(Model model) {
        populateDashboardModel(model);
        return "admin/staff-dashboard";
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(name = "period", defaultValue = "month") String period,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            Model model) {
        if ("custom".equals(period) && startDate != null && endDate != null) {
            model.addAttribute("report", reportService.getManagerReportByDateRange(
                    LocalDate.parse(startDate), LocalDate.parse(endDate)));
        } else {
            model.addAttribute("report", reportService.getManagerReport(period));
        }
        model.addAttribute("period", period);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/reports";
    }

    /**
     * REST API: trả về số lượng đơn hàng theo từng ngày trong khoảng thời gian tùy chọn.
     * Dùng cho biểu đồ thống kê ở trang Báo cáo.
     */
    @GetMapping("/reports/order-stats")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> orderStats(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            if (end.isBefore(start)) {
                return ResponseEntity.badRequest().build();
            }
            List<Map<String, Object>> data = reportService.getOrderCountByDateRange(start, end);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(name = "period", defaultValue = "month") String period,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate) {
        byte[] bytes;
        String fileName;
        if ("custom".equals(period) && startDate != null && endDate != null) {
            bytes = reportService.exportManagerReportExcelByDateRange(
                    LocalDate.parse(startDate), LocalDate.parse(endDate));
            fileName = "phoneshop-report-" + startDate + "_" + endDate + ".xlsx";
        } else {
            bytes = reportService.exportManagerReportExcel(period);
            fileName = "phoneshop-report-" + period + ".xlsx";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }

    private long countOrders(List<Order> orders, OrderStatus status) {
        return orders.stream()
                .filter(order -> order != null && order.getStatus() == status)
                .count();
    }
}
