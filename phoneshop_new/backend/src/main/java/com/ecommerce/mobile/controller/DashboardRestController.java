package com.ecommerce.mobile.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.dto.dashboard.DrillDownOrderDto;
import com.ecommerce.mobile.dto.dashboard.OperationalDashboardDto;
import com.ecommerce.mobile.service.OperationalDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardRestController {

    private final OperationalDashboardService dashboardService;

    public DashboardRestController(OperationalDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/charts")
    public ResponseEntity<OperationalDashboardDto> getChartData() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    @GetMapping("/drill-down/status")
    public ResponseEntity<List<DrillDownOrderDto>> drillDownByStatus(@RequestParam String status) {
        return ResponseEntity.ok(dashboardService.getDrillDownOrdersByStatus(status));
    }

    @GetMapping("/drill-down/product")
    public ResponseEntity<List<DrillDownOrderDto>> drillDownByProduct(@RequestParam String productName) {
        return ResponseEntity.ok(dashboardService.getDrillDownOrdersByProduct(productName));
    }
}
