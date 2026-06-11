package com.ecommerce.mobile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.ecommerce.mobile.service.OlapDashboardService;

@Controller
@RequestMapping("/admin")
public class OlapDashboardController {

    private final OlapDashboardService olapDashboardService;

    public OlapDashboardController(OlapDashboardService olapDashboardService) {
        this.olapDashboardService = olapDashboardService;
    }

    @GetMapping("/analytics")
    public String analytics(
            @RequestParam(name = "grain", defaultValue = "day") String grain,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            Model model) {
        model.addAttribute("dashboard", olapDashboardService.getDashboard(grain, startDate, endDate));
        return "admin/analytics";
    }

    @GetMapping("/analytics/export-mismatched")
    public ResponseEntity<byte[]> exportMismatchedPayments(
            @RequestParam(name = "grain", defaultValue = "day") String grain,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate) {
        
        byte[] excelContent = olapDashboardService.exportMismatchedPaymentsToExcel(grain, startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "doi-soat-loi.xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelContent);
    }

    @org.springframework.web.bind.annotation.PostMapping("/dwh/sync")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<String> syncDwh() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String pythonCommand = os.contains("win") ? "python" : "python3";
            java.io.File workingDir = new java.io.File("../olap").getAbsoluteFile();
            if (!workingDir.exists() || !workingDir.isDirectory()) {
                workingDir = new java.io.File("olap").getAbsoluteFile(); // fallback if running from parent
            }

            ProcessBuilder pb = new ProcessBuilder(pythonCommand, "etl_target_pipeline.py");
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return org.springframework.http.ResponseEntity.ok("ETL completed successfully.\n" + output.toString());
            } else {
                return org.springframework.http.ResponseEntity.status(500).body("ETL failed with exit code " + exitCode + ".\n" + output.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.status(500).body("Failed to trigger ETL: " + e.getMessage());
        }
    }
}
