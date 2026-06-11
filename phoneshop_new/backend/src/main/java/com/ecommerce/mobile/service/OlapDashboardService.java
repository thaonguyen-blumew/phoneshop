package com.ecommerce.mobile.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import com.ecommerce.mobile.dto.olap.OlapDashboardView;
import com.ecommerce.mobile.dto.olap.OlapDashboardView.BreakdownPoint;
import com.ecommerce.mobile.dto.olap.OlapDashboardView.MismatchedPayment;
import com.ecommerce.mobile.dto.olap.OlapDashboardView.ProductPerformance;
import com.ecommerce.mobile.dto.olap.OlapDashboardView.ReviewWarning;
import com.ecommerce.mobile.dto.olap.OlapDashboardView.Summary;
import com.ecommerce.mobile.dto.olap.OlapDashboardView.TrendPoint;

@Service
public class OlapDashboardService {

    private static final List<String> REQUIRED_TABLES = List.of(
            "Dim_Date",
            "Dim_Product",
            "Dim_Customer",
            "Fact_Sales",
            "Fact_Payments",
            "Fact_Inventory",
            "Fact_Reviews");
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

    private final NamedParameterJdbcTemplate jdbc;

    public OlapDashboardService(
            @Value("${olap.datasource.url:jdbc:mysql://localhost:3306/phoneshop_dwh?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh}") String url,
            @Value("${olap.datasource.username:${APP_DB_USERNAME:root}}") String username,
            @Value("${olap.datasource.password:${APP_DB_PASSWORD:root}}") String password,
            @Value("${olap.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}") String driverClassName) {
        com.zaxxer.hikari.HikariDataSource dataSource = new com.zaxxer.hikari.HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);
        dataSource.setIdleTimeout(30000);
        dataSource.setConnectionTimeout(20000);
        this.jdbc = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource));
    }

    public OlapDashboardView getDashboard(String grainRaw, String startDateStr, String endDateStr) {
        OlapDashboardView view = new OlapDashboardView();
        String grain = normalizeGrain(grainRaw);
        
        LocalDate start = null;
        LocalDate end = null;
        try {
            if (startDateStr != null && !startDateStr.isBlank()) {
                start = LocalDate.parse(startDateStr);
            }
            if (endDateStr != null && !endDateStr.isBlank()) {
                end = LocalDate.parse(endDateStr);
            }
        } catch (Exception e) {
            // Keep null if parsing fails
        }

        if (start == null || end == null) {
            DateWindow window = resolveCurrentPeriod(grain);
            if (start == null) start = window.start();
            if (end == null) end = window.end();
        }

        view.setGrain(grain);
        view.setStartDate(start);
        view.setEndDate(end);
        view.setPeriodLabel(periodLabel(grain, start, end));

        try {
            List<String> missingTables = findMissingRequiredTables();
            if (!missingTables.isEmpty()) {
                view.setAvailable(false);
                view.setMessage("DWH phoneshop_dwh thiếu bảng: " + String.join(", ", missingTables)
                        + ". Hãy chạy schema và ETL trước khi xem dashboard OLAP.");
                return view;
            }

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("startDate", start)
                    .addValue("endDate", end)
                    .addValue("endDateKey", dateKey(end));

            view.setSalesTrend(loadSalesTrend(grain, params));
            view.setTopProducts(loadTopProducts(params));
            view.setSlowMovingProducts(loadSlowMovingProducts(params));
            view.setRevenueByBrand(loadRevenueByBrand(params));
            view.setRevenueByCategory(loadRevenueByCategory(params));
            view.setRevenueByCustomerSegment(loadRevenueByCustomerSegment(params));
            view.setInventoryByCategory(loadInventoryByCategory(params));
            view.setPaymentReconciliation(loadPaymentReconciliation(params));
            view.setRatingDistribution(loadRatingDistribution(params));
            view.setReviewWarnings(loadReviewWarnings(params));
            view.setMismatchedPayments(loadMismatchedPayments(params));
            view.setSummary(loadSummary(params, view.getSlowMovingProducts().size()));
            view.setMessage("Dữ liệu lấy từ phoneshop_dwh. " + view.getPeriodLabel() + ".");
        } catch (DataAccessException ex) {
            view.setAvailable(false);
            view.setMessage("Lỗi đọc DWH phoneshop_dwh: " + shortError(ex));
        }

        return view;
    }

    private Summary loadSummary(MapSqlParameterSource params, int slowMovingCount) {
        Summary summary = queryOne("""
                SELECT
                    COALESCE(SUM(s.quantity * s.unit_price), 0) AS total_revenue,
                    COALESCE(SUM(s.gross_profit), 0) AS gross_profit,
                    COALESCE(SUM(s.net_profit), 0) AS net_profit,
                    COALESCE(SUM(s.quantity), 0) AS units_sold
                FROM Fact_Sales s
                JOIN Dim_Date d ON d.date_key = s.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                """, params, (rs, rowNum) -> {
            Summary s = new Summary();
            BigDecimal revenue = money(rs.getBigDecimal("total_revenue"));
            BigDecimal grossProfit = money(rs.getBigDecimal("gross_profit"));
            BigDecimal netProfit = money(rs.getBigDecimal("net_profit"));
            s.setTotalRevenue(revenue);
            s.setGrossProfit(grossProfit);
            s.setNetProfit(netProfit);
            s.setUnitsSold(rs.getLong("units_sold"));
            s.setGrossMarginRate(percent(grossProfit, revenue));
            s.setNetMarginRate(percent(netProfit, revenue));
            return s;
        }, new Summary());

        queryOne("""
                SELECT
                    COUNT(DISTINCT order_id) AS order_count,
                    COALESCE(SUM(net_received), 0) AS net_received,
                    COALESCE(SUM(CASE WHEN reconciliation_status = 'MATCHED' THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0) * 100, 0) AS match_rate
                FROM Fact_Payments p
                JOIN Dim_Date d ON d.date_key = p.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                """, params, (rs, rowNum) -> {
            summary.setOrderCount(rs.getLong("order_count"));
            summary.setNetReceived(money(rs.getBigDecimal("net_received")));
            summary.setPaymentMatchRate(rate(rs.getBigDecimal("match_rate")));
            return summary;
        }, summary);

        queryOne("""
                SELECT
                    COALESCE(SUM(i.quantity_on_hand), 0) AS inventory_quantity,
                    COALESCE(SUM(i.inventory_value), 0) AS inventory_value,
                    COALESCE(SUM(CASE WHEN i.quantity_on_hand <= 5 THEN 1 ELSE 0 END), 0) AS low_stock
                FROM Fact_Inventory i
                JOIN (
                    SELECT product_key, MAX(date_key) AS latest_date_key
                    FROM Fact_Inventory
                    WHERE date_key <= :endDateKey
                    GROUP BY product_key
                ) latest ON latest.product_key = i.product_key AND latest.latest_date_key = i.date_key
                """, params, (rs, rowNum) -> {
            summary.setInventoryQuantity(rs.getLong("inventory_quantity"));
            summary.setInventoryValue(money(rs.getBigDecimal("inventory_value")));
            summary.setLowStockProducts(rs.getLong("low_stock"));
            return summary;
        }, summary);

        queryOne("""
                SELECT
                    COALESCE(AVG(r.rating_stars), 0) AS average_rating,
                    COALESCE(SUM(CASE WHEN r.is_verified = TRUE THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0) * 100, 0) AS verified_rate
                FROM Fact_Reviews r
                JOIN Dim_Date d ON d.date_key = r.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                """, params, (rs, rowNum) -> {
            summary.setAverageRating(rate(rs.getBigDecimal("average_rating")));
            summary.setVerifiedReviewRate(rate(rs.getBigDecimal("verified_rate")));
            return summary;
        }, summary);

        summary.setSlowMovingProducts(slowMovingCount);
        return summary;
    }

    private List<TrendPoint> loadSalesTrend(String grain, MapSqlParameterSource params) {
        String periodExpr = switch (grain) {
            case "week" -> "CONCAT(d.year, '-W', LPAD(WEEK(d.full_date, 3), 2, '0'))";
            case "month" -> "CONCAT(d.year, '-', LPAD(d.month, 2, '0'))";
            default -> "DATE_FORMAT(d.full_date, '%d/%m/%Y')";
        };
        String groupExpr = switch (grain) {
            case "week" -> "d.year, WEEK(d.full_date, 3)";
            case "month" -> "d.year, d.month";
            default -> "d.full_date";
        };

        return jdbc.query("""
                SELECT
                    %s AS label,
                    COALESCE(SUM(s.quantity * s.unit_price), 0) AS total_revenue,
                    COALESCE(SUM(s.gross_profit), 0) AS gross_profit,
                    COALESCE(SUM(s.net_profit), 0) AS net_profit,
                    COALESCE(SUM(s.quantity), 0) AS units_sold
                FROM Fact_Sales s
                JOIN Dim_Date d ON d.date_key = s.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                GROUP BY %s
                ORDER BY MIN(d.full_date)
                """.formatted(periodExpr, groupExpr), params, (rs, rowNum) -> {
            TrendPoint point = new TrendPoint();
            point.setLabel(rs.getString("label"));
            point.setTotalRevenue(money(rs.getBigDecimal("total_revenue")));
            point.setGrossProfit(money(rs.getBigDecimal("gross_profit")));
            point.setNetProfit(money(rs.getBigDecimal("net_profit")));
            point.setUnitsSold(rs.getLong("units_sold"));
            return point;
        });
    }

    private List<ProductPerformance> loadTopProducts(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    p.product_name,
                    COALESCE(p.brand, 'Khác') AS brand,
                    COALESCE(p.category_name, 'Khác') AS category_name,
                    COALESCE(SUM(s.quantity), 0) AS units_sold,
                    COALESCE(SUM(s.quantity * s.unit_price), 0) AS total_revenue,
                    COALESCE(SUM(s.net_profit), 0) AS net_profit
                FROM Fact_Sales s
                JOIN Dim_Product p ON p.product_key = s.product_key
                JOIN Dim_Date d ON d.date_key = s.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                GROUP BY p.product_key, p.product_name, p.brand, p.category_name
                ORDER BY total_revenue DESC, units_sold DESC
                LIMIT 10
                """, params, (rs, rowNum) -> {
            ProductPerformance product = new ProductPerformance();
            product.setProductName(rs.getString("product_name"));
            product.setBrand(rs.getString("brand"));
            product.setCategoryName(rs.getString("category_name"));
            product.setUnitsSold(rs.getLong("units_sold"));
            product.setTotalRevenue(money(rs.getBigDecimal("total_revenue")));
            product.setNetProfit(money(rs.getBigDecimal("net_profit")));
            product.setSuggestedAction("Duy trì bán");
            return product;
        });
    }

    private List<ProductPerformance> loadSlowMovingProducts(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    p.product_name,
                    COALESCE(p.brand, 'Khác') AS brand,
                    COALESCE(p.category_name, 'Khác') AS category_name,
                    COALESCE(inv.quantity_on_hand, 0) AS quantity_on_hand,
                    COALESCE(sales.units_sold, 0) AS units_sold,
                    COALESCE(inv.inventory_value, 0) AS inventory_value,
                    COALESCE(inv.quantity_on_hand, 0) / GREATEST(COALESCE(sales.units_sold, 0), 1) AS slow_moving_score
                FROM Dim_Product p
                LEFT JOIN (
                    SELECT s.product_key, SUM(s.quantity) AS units_sold
                    FROM Fact_Sales s
                    JOIN Dim_Date d ON d.date_key = s.date_key
                    WHERE d.full_date BETWEEN :startDate AND :endDate
                    GROUP BY s.product_key
                ) sales ON sales.product_key = p.product_key
                LEFT JOIN (
                    SELECT i.product_key, i.quantity_on_hand, i.inventory_value
                    FROM Fact_Inventory i
                    JOIN (
                        SELECT product_key, MAX(date_key) AS latest_date_key
                        FROM Fact_Inventory
                        WHERE date_key <= :endDateKey
                        GROUP BY product_key
                    ) latest ON latest.product_key = i.product_key AND latest.latest_date_key = i.date_key
                ) inv ON inv.product_key = p.product_key
                WHERE COALESCE(inv.quantity_on_hand, 0) > 0
                ORDER BY units_sold ASC, slow_moving_score DESC, inventory_value DESC
                LIMIT 20
                """, params, (rs, rowNum) -> {
            ProductPerformance product = new ProductPerformance();
            product.setProductName(rs.getString("product_name"));
            product.setBrand(rs.getString("brand"));
            product.setCategoryName(rs.getString("category_name"));
            product.setQuantityOnHand(rs.getLong("quantity_on_hand"));
            product.setUnitsSold(rs.getLong("units_sold"));
            product.setInventoryValue(money(rs.getBigDecimal("inventory_value")));
            product.setSlowMovingScore(rate(rs.getBigDecimal("slow_moving_score")));
            product.setSuggestedAction(slowMovingAction(product.getQuantityOnHand(), product.getUnitsSold()));
            return product;
        });
    }

    private List<BreakdownPoint> loadRevenueByBrand(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    COALESCE(p.brand, 'Khác') AS label,
                    COALESCE(SUM(s.quantity * s.unit_price), 0) AS value
                FROM Fact_Sales s
                JOIN Dim_Product p ON p.product_key = s.product_key
                JOIN Dim_Date d ON d.date_key = s.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                GROUP BY p.brand
                ORDER BY value DESC
                LIMIT 8
                """, params, (rs, rowNum) -> breakdown(rs.getString("label"), rs.getBigDecimal("value")));
    }

    private List<BreakdownPoint> loadRevenueByCategory(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    COALESCE(p.category_name, 'Khác') AS label,
                    COALESCE(SUM(s.quantity * s.unit_price), 0) AS value
                FROM Fact_Sales s
                JOIN Dim_Product p ON p.product_key = s.product_key
                JOIN Dim_Date d ON d.date_key = s.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                GROUP BY p.category_name
                ORDER BY value DESC
                LIMIT 8
                """, params, (rs, rowNum) -> breakdown(rs.getString("label"), rs.getBigDecimal("value")));
    }

    private List<BreakdownPoint> loadRevenueByCustomerSegment(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    COALESCE(c.customer_segment, 'Khác') AS label,
                    COALESCE(SUM(s.quantity * s.unit_price), 0) AS value
                FROM Fact_Sales s
                JOIN Dim_Customer c ON c.customer_key = s.customer_key
                JOIN Dim_Date d ON d.date_key = s.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                GROUP BY c.customer_segment
                ORDER BY value DESC
                LIMIT 8
                """, params, (rs, rowNum) -> breakdown(rs.getString("label"), rs.getBigDecimal("value")));
    }

    private List<BreakdownPoint> loadInventoryByCategory(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    COALESCE(p.category_name, 'Khác') AS label,
                    COALESCE(SUM(i.inventory_value), 0) AS value
                FROM Fact_Inventory i
                JOIN Dim_Product p ON p.product_key = i.product_key
                JOIN (
                    SELECT product_key, MAX(date_key) AS latest_date_key
                    FROM Fact_Inventory
                    WHERE date_key <= :endDateKey
                    GROUP BY product_key
                ) latest ON latest.product_key = i.product_key AND latest.latest_date_key = i.date_key
                GROUP BY p.category_name
                ORDER BY value DESC
                LIMIT 8
                """, params, (rs, rowNum) -> breakdown(rs.getString("label"), rs.getBigDecimal("value")));
    }

    private List<BreakdownPoint> loadPaymentReconciliation(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    reconciliation_status AS label,
                    COUNT(*) AS value
                FROM Fact_Payments p
                JOIN Dim_Date d ON d.date_key = p.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                GROUP BY reconciliation_status
                ORDER BY value DESC
                """, params, (rs, rowNum) -> breakdown(rs.getString("label"), rs.getBigDecimal("value")));
    }

    private List<BreakdownPoint> loadRatingDistribution(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    CONCAT(r.rating_stars, ' sao') AS label,
                    COUNT(*) AS value
                FROM Fact_Reviews r
                JOIN Dim_Date d ON d.date_key = r.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                GROUP BY r.rating_stars
                ORDER BY r.rating_stars
                """, params, (rs, rowNum) -> breakdown(rs.getString("label"), rs.getBigDecimal("value")));
    }

    private List<ReviewWarning> loadReviewWarnings(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT
                    p.product_name,
                    COALESCE(p.brand, 'Khác') AS brand,
                    COUNT(r.review_key) AS review_count,
                    COALESCE(AVG(r.rating_stars), 0) AS average_rating
                FROM Fact_Reviews r
                JOIN Dim_Product p ON p.product_key = r.product_key
                JOIN Dim_Date d ON d.date_key = r.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                GROUP BY p.product_key, p.product_name, p.brand
                HAVING AVG(r.rating_stars) < 3.5
                ORDER BY average_rating ASC, review_count DESC
                LIMIT 10
                """, params, (rs, rowNum) -> {
            ReviewWarning warning = new ReviewWarning();
            warning.setProductName(rs.getString("product_name"));
            warning.setBrand(rs.getString("brand"));
            warning.setReviewCount(rs.getLong("review_count"));
            warning.setAverageRating(rate(rs.getBigDecimal("average_rating")));
            return warning;
        });
    }

    private List<MismatchedPayment> loadMismatchedPayments(MapSqlParameterSource params) {
        return jdbc.query("""
                SELECT 
                    p.order_id,
                    p.payment_time,
                    c.full_name,
                    p.amount,
                    p.gateway_fee,
                    p.net_received,
                    p.reconciliation_status
                FROM Fact_Payments p
                JOIN Dim_Customer c ON c.customer_key = p.customer_key
                JOIN Dim_Date d ON d.date_key = p.date_key
                WHERE d.full_date BETWEEN :startDate AND :endDate
                  AND p.reconciliation_status != 'MATCHED'
                ORDER BY p.payment_time DESC
                """, params, (rs, rowNum) -> {
            MismatchedPayment mismatched = new MismatchedPayment();
            mismatched.setOrderId(rs.getLong("order_id"));
            java.sql.Timestamp timestamp = rs.getTimestamp("payment_time");
            if (timestamp != null) {
                mismatched.setPaymentTime(timestamp.toLocalDateTime());
            }
            mismatched.setCustomerName(rs.getString("full_name"));
            mismatched.setAmount(money(rs.getBigDecimal("amount")));
            mismatched.setGatewayFee(money(rs.getBigDecimal("gateway_fee")));
            mismatched.setNetReceived(money(rs.getBigDecimal("net_received")));
            mismatched.setReconciliationStatus(rs.getString("reconciliation_status"));
            return mismatched;
        });
    }

    public byte[] exportMismatchedPaymentsToExcel(String grainRaw, String startDateStr, String endDateStr) {
        String grain = normalizeGrain(grainRaw);
        LocalDate start = null;
        LocalDate end = null;
        try {
            if (startDateStr != null && !startDateStr.isBlank()) start = LocalDate.parse(startDateStr);
            if (endDateStr != null && !endDateStr.isBlank()) end = LocalDate.parse(endDateStr);
        } catch (Exception e) {}

        if (start == null || end == null) {
            DateWindow window = resolveCurrentPeriod(grain);
            if (start == null) start = window.start();
            if (end == null) end = window.end();
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startDate", start)
                .addValue("endDate", end);
                
        List<MismatchedPayment> payments = loadMismatchedPayments(params);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Đối soát lỗi");
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Thời gian", "Mã ĐH", "Khách hàng", "Số tiền", "Phí cổng", "Thực nhận", "Trạng thái"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (MismatchedPayment p : payments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getPaymentTime() != null ? p.getPaymentTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) : "");
                row.createCell(1).setCellValue(p.getOrderId() != null ? p.getOrderId() : 0);
                row.createCell(2).setCellValue(p.getCustomerName() != null ? p.getCustomerName() : "");
                row.createCell(3).setCellValue(p.getAmount() != null ? p.getAmount().doubleValue() : 0);
                row.createCell(4).setCellValue(p.getGatewayFee() != null ? p.getGatewayFee().doubleValue() : 0);
                row.createCell(5).setCellValue(p.getNetReceived() != null ? p.getNetReceived().doubleValue() : 0);
                row.createCell(6).setCellValue(p.getReconciliationStatus() != null ? p.getReconciliationStatus() : "");
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file Excel", e);
        }
    }

    private <T> T queryOne(String sql, MapSqlParameterSource params, org.springframework.jdbc.core.RowMapper<T> mapper, T fallback) {
        List<T> rows = jdbc.query(sql, params, mapper);
        return rows.isEmpty() ? fallback : rows.get(0);
    }

    private BreakdownPoint breakdown(String label, BigDecimal value) {
        BreakdownPoint point = new BreakdownPoint();
        point.setLabel(label == null || label.isBlank() ? "Khác" : label);
        point.setValue(money(value));
        return point;
    }

    private String normalizeGrain(String raw) {
        if (raw == null || raw.isBlank()) {
            return "day";
        }
        return switch (raw.trim().toLowerCase()) {
            case "week", "tuan", "tuần" -> "week";
            case "month", "thang", "tháng" -> "month";
            default -> "day";
        };
    }

    private DateWindow resolveCurrentPeriod(String grain) {
        LocalDate today = LocalDate.now();
        return switch (grain) {
            case "week" -> new DateWindow(
                    today.minusWeeks(12),
                    today);
            case "month" -> new DateWindow(
                    today.withDayOfYear(1),
                    today.withDayOfYear(today.lengthOfYear()));
            default -> new DateWindow(
                    today.withDayOfMonth(1),
                    today.withDayOfMonth(today.lengthOfMonth()));
        };
    }

    private String periodLabel(String grain, LocalDate start, LocalDate end) {
        String grainLabel = switch (grain) {
            case "week" -> "Theo tuần";
            case "month" -> "Theo tháng";
            default -> "Theo ngày";
        };
        return grainLabel + ": " + start.format(PERIOD_FORMATTER) + " - " + end.format(PERIOD_FORMATTER);
    }

    private int dateKey(LocalDate date) {
        return date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
    }

    private List<String> findMissingRequiredTables() {
        List<String> existingTables = jdbc.query("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (:tables)
                """, new MapSqlParameterSource("tables", REQUIRED_TABLES), (rs, rowNum) -> rs.getString("table_name"));
        return REQUIRED_TABLES.stream()
                .filter(required -> existingTables.stream().noneMatch(existing -> required.equalsIgnoreCase(existing)))
                .toList();
    }

    private String slowMovingAction(long quantityOnHand, long unitsSold) {
        if (unitsSold == 0 && quantityOnHand >= 10) {
            return "Đẩy khuyến mãi";
        }
        if (unitsSold == 0) {
            return "Theo dõi";
        }
        if (quantityOnHand >= 10) {
            return "Kích cầu";
        }
        return "Theo dõi";
    }

    private String shortError(DataAccessException ex) {
        String message = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
        if (message == null || message.isBlank()) {
            return "không kết nối hoặc truy vấn được DWH.";
        }
        return message.length() > 240 ? message.substring(0, 240) + "..." : message;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private record DateWindow(LocalDate start, LocalDate end) {
    }
}
