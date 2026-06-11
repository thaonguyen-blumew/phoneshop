package com.ecommerce.mobile.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.mobile.dto.report.CategorySalesRow;
import com.ecommerce.mobile.dto.report.ManagerReportSummary;
import com.ecommerce.mobile.dto.report.ManagerReportView;
import com.ecommerce.mobile.dto.report.ReportPeriodPoint;
import com.ecommerce.mobile.dto.report.StockAlertRow;
import com.ecommerce.mobile.dto.report.TopProductRow;
import com.ecommerce.mobile.entity.Category;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.OrderItem;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.entity.ProductVariant;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.enums.ProductStatus;
import com.ecommerce.mobile.repository.OrderRepository;
import com.ecommerce.mobile.repository.ProductRepository;
import com.ecommerce.mobile.repository.ProductVariantRepository;
import com.ecommerce.mobile.repository.CustomerRepository;
import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.dto.report.BrandSalesRow;
import com.ecommerce.mobile.dto.report.TopCustomerRow;

@Service
public class ReportService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CustomerRepository customerRepository;

    public ReportService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         ProductVariantRepository productVariantRepository,
                         CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public ManagerReportView getManagerReport() {
        return getManagerReport("month");
    }

    @Transactional(readOnly = true)
    public ManagerReportView getManagerReport(String periodTypeRaw) {
        ReportPeriodType periodType = ReportPeriodType.from(periodTypeRaw);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        if ("day".equalsIgnoreCase(periodTypeRaw)) {
            startDate = now.toLocalDate().atStartOfDay();
        } else if ("week".equalsIgnoreCase(periodTypeRaw)) {
            startDate = now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        } else if ("year".equalsIgnoreCase(periodTypeRaw)) {
            startDate = now.toLocalDate().withDayOfYear(1).atStartOfDay();
        } else if ("all".equalsIgnoreCase(periodTypeRaw)) {
            startDate = LocalDateTime.of(2000, 1, 1, 0, 0); // Lấy từ rất lâu trước đây
        } else {
            startDate = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        }

        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        
        List<Order> filteredOrders = allOrders.stream()
                .filter(order -> order != null && order.getCreatedAt() != null && !order.getCreatedAt().isBefore(startDate))
                .toList();

        List<Order> reportableOrders = filteredOrders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();
        List<Order> deliveredOrders = filteredOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .toList();

        List<Product> allProducts = productRepository.findAll();
        List<ProductVariant> allVariants = productVariantRepository.findAll();
        List<Customer> allCustomers = customerRepository.findAll();

        ManagerReportSummary summary = buildSummary(filteredOrders, reportableOrders, deliveredOrders, allProducts, allVariants, allCustomers, startDate);
        List<ReportPeriodPoint> periodPoints = buildPeriodPoints(reportableOrders, deliveredOrders, periodType);
        List<CategorySalesRow> categorySales = buildCategorySales(deliveredOrders);
        List<TopProductRow> topProducts = buildTopProducts(deliveredOrders);
        List<StockAlertRow> stockAlerts = buildStockAlerts(allVariants);
        List<BrandSalesRow> brandSales = buildBrandSales(deliveredOrders);
        List<TopCustomerRow> topCustomers = buildTopCustomers(deliveredOrders);

        ManagerReportView view = new ManagerReportView();
        view.setPeriodType(periodType.code);
        view.setPeriodLabel(periodType.displayLabel);
        view.setSummary(summary);
        view.setPeriodPoints(periodPoints);
        view.setCategorySales(categorySales);
        view.setTopProducts(topProducts);
        view.setStockAlerts(stockAlerts);
        view.setBrandSales(brandSales);
        view.setTopCustomers(topCustomers);
        return view;
    }

    @Transactional(readOnly = true)
    public byte[] exportManagerReportExcel(String periodTypeRaw) {
        ManagerReportView view = getManagerReport(periodTypeRaw);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSummarySheet(workbook, view);
            writePeriodSheet(workbook, view);
            writeCategorySalesSheet(workbook, view);
            writeTopProductsSheet(workbook, view);
            writeStockAlertsSheet(workbook, view);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể xuất báo cáo Excel", ex);
        }
    }

    /**
     * Lấy số lượng đơn hàng theo từng ngày trong khoảng thời gian [startDate, endDate].
     * Trả về list các map gồm: date (String), total (long), delivered (long), cancelled (long), other (long).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrderCountByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDate.plusDays(1).atStartOfDay();

        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault());

        // Nhóm theo ngày
        Map<LocalDate, List<Order>> byDay = allOrders.stream()
                .filter(o -> o != null && o.getCreatedAt() != null
                        && !o.getCreatedAt().isBefore(startDt)
                        && o.getCreatedAt().isBefore(endDt))
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            List<Order> dayOrders = byDay.getOrDefault(cursor, List.of());
            long total = dayOrders.size();
            long delivered = dayOrders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
            long cancelled = dayOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
            long other = total - delivered - cancelled;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", cursor.toString());          // yyyy-MM-dd
            point.put("label", cursor.format(fmt));        // dd/MM
            point.put("total", total);
            point.put("delivered", delivered);
            point.put("cancelled", cancelled);
            point.put("other", other);
            result.add(point);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * Báo cáo tổng quát theo khoảng ngày tùy chọn (dùng cho Thymeleaf page).
     */
    @Transactional(readOnly = true)
    public ManagerReportView getManagerReportByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDate.plusDays(1).atStartOfDay();

        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<Order> filteredOrders = allOrders.stream()
                .filter(order -> order != null && order.getCreatedAt() != null
                        && !order.getCreatedAt().isBefore(startDt)
                        && order.getCreatedAt().isBefore(endDt))
                .toList();

        List<Order> reportableOrders = filteredOrders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();
        List<Order> deliveredOrders = filteredOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .toList();

        List<com.ecommerce.mobile.entity.Product> allProducts = productRepository.findAll();
        List<ProductVariant> allVariants = productVariantRepository.findAll();
        List<com.ecommerce.mobile.entity.Customer> allCustomers = customerRepository.findAll();

        ManagerReportSummary summary = buildSummary(filteredOrders, reportableOrders, deliveredOrders,
                allProducts, allVariants, allCustomers, startDt);

        // Period points theo từng ngày trong khoảng
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM", Locale.getDefault());
        Map<LocalDate, ReportPeriodPoint> pointMap = new LinkedHashMap<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            ReportPeriodPoint p = new ReportPeriodPoint();
            p.setLabel(cursor.format(fmt));
            pointMap.put(cursor, p);
            cursor = cursor.plusDays(1);
        }
        for (Order order : reportableOrders) {
            LocalDate d = order.getCreatedAt().toLocalDate();
            ReportPeriodPoint p = pointMap.get(d);
            if (p != null) {
                p.setOrders(p.getOrders() + 1);
                p.setGrossRevenue(p.getGrossRevenue().add(safeBigDecimal(order.getTotalAmount())));
            }
        }
        for (Order order : deliveredOrders) {
            LocalDate d = order.getCreatedAt().toLocalDate();
            ReportPeriodPoint p = pointMap.get(d);
            if (p != null) {
                p.setRealizedRevenue(p.getRealizedRevenue().add(safeBigDecimal(order.getTotalAmount())));
                p.setEstimatedProfit(p.getEstimatedProfit().add(calculateOrderProfit(order)));
            }
        }

        ManagerReportView view = new ManagerReportView();
        view.setPeriodType("custom");
        view.setPeriodLabel(startDate + " - " + endDate);
        view.setSummary(summary);
        view.setPeriodPoints(new ArrayList<>(pointMap.values()));
        view.setCategorySales(buildCategorySales(deliveredOrders));
        view.setTopProducts(buildTopProducts(deliveredOrders));
        view.setStockAlerts(buildStockAlerts(allVariants));
        view.setBrandSales(buildBrandSales(deliveredOrders));
        view.setTopCustomers(buildTopCustomers(deliveredOrders));
        return view;
    }

    /**
     * Xuất Excel theo khoảng ngày tùy chọn.
     */
    @Transactional(readOnly = true)
    public byte[] exportManagerReportExcelByDateRange(LocalDate startDate, LocalDate endDate) {
        ManagerReportView view = getManagerReportByDateRange(startDate, endDate);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSummarySheet(workbook, view);
            writePeriodSheet(workbook, view);
            writeCategorySalesSheet(workbook, view);
            writeTopProductsSheet(workbook, view);
            writeStockAlertsSheet(workbook, view);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể xuất báo cáo Excel", ex);
        }
    }

    private ManagerReportSummary buildSummary(List<Order> filteredOrders,
                                              List<Order> reportableOrders,
                                              List<Order> deliveredOrders,
                                              List<Product> allProducts,
                                              List<ProductVariant> allVariants,
                                              List<Customer> allCustomers,
                                              LocalDateTime startDate) {
        ManagerReportSummary summary = new ManagerReportSummary();
        summary.setTotalOrders(reportableOrders.size());
        summary.setDeliveredOrders(deliveredOrders.size());
        summary.setCancelledOrders(filteredOrders.stream()
                .filter(order -> order != null && order.getStatus() == OrderStatus.CANCELLED)
                .count());
        summary.setActiveProducts((long) allProducts.stream()
                .filter(product -> product != null && product.getStatus() == ProductStatus.ACTIVE)
                .count());
        summary.setTotalVariants(allVariants.size());
        summary.setLowStockVariants(allVariants.stream().filter(this::isLowStock).count());
        summary.setOutOfStockVariants(allVariants.stream().filter(v -> v != null && v.getStockQty() != null && v.getStockQty() == 0).count());
        summary.setTotalStockQty(allVariants.stream()
                .mapToLong(variant -> variant == null || variant.getStockQty() == null ? 0L : variant.getStockQty())
                .sum());
        summary.setTotalCustomers(allCustomers.size());
        summary.setNewCustomers(allCustomers.stream()
                .filter(c -> c.getCreatedAt() != null && !c.getCreatedAt().isBefore(startDate))
                .count());

        long totalProductsSold = 0;
        for (Order o : deliveredOrders) {
            if (o != null && o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    totalProductsSold += (item != null && item.getQuantity() != null) ? item.getQuantity() : 0;
                }
            }
        }
        summary.setTotalProductsSold(totalProductsSold);

        BigDecimal grossRevenue = sumOrders(reportableOrders);
        BigDecimal realizedRevenue = sumOrders(deliveredOrders);
        BigDecimal realizedCost = sumDeliveryCost(deliveredOrders);
        BigDecimal inventoryValue = sumInventoryValue(allVariants);

        summary.setGrossRevenue(grossRevenue);
        summary.setRealizedRevenue(realizedRevenue);
        summary.setEstimatedCost(realizedCost);
        summary.setEstimatedProfit(realizedRevenue.subtract(realizedCost));
        summary.setGrossProfitMargin(calculateMargin(realizedRevenue, summary.getEstimatedProfit()));
        summary.setInventoryValue(inventoryValue);
        return summary;
    }

    private BigDecimal calculateMargin(BigDecimal revenue, BigDecimal profit) {
        if (revenue == null || revenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return profit.divide(revenue, 4, RoundingMode.HALF_UP);
    }

    private List<ReportPeriodPoint> buildPeriodPoints(List<Order> reportableOrders,
                                                      List<Order> deliveredOrders,
                                                      ReportPeriodType periodType) {
        Map<String, ReportPeriodPoint> points = new LinkedHashMap<>();
        for (PeriodSlot slot : buildPeriodSlots(periodType)) {
            ReportPeriodPoint point = new ReportPeriodPoint();
            point.setLabel(slot.label());
            points.put(slot.key(), point);
        }

        for (Order order : reportableOrders) {
            if (order == null || order.getCreatedAt() == null) {
                continue;
            }
            String key = periodKey(order.getCreatedAt(), periodType);
            ReportPeriodPoint point = points.get(key);
            if (point != null) {
                point.setOrders(point.getOrders() + 1);
                point.setGrossRevenue(point.getGrossRevenue().add(safeBigDecimal(order.getTotalAmount())));
            }
        }

        for (Order order : deliveredOrders) {
            if (order == null || order.getCreatedAt() == null) {
                continue;
            }
            String key = periodKey(order.getCreatedAt(), periodType);
            ReportPeriodPoint point = points.get(key);
            if (point != null) {
                point.setRealizedRevenue(point.getRealizedRevenue().add(safeBigDecimal(order.getTotalAmount())));
                point.setEstimatedProfit(point.getEstimatedProfit().add(calculateOrderProfit(order)));
            }
        }

        return new ArrayList<>(points.values());
    }

    private List<PeriodSlot> buildPeriodSlots(ReportPeriodType periodType) {
        List<PeriodSlot> slots = new ArrayList<>();
        switch (periodType) {
            case DAY -> {
                for (int i = 0; i <= 23; i++) {
                    String hourStr = String.format("%02d:00", i);
                    slots.add(new PeriodSlot(String.valueOf(i), hourStr));
                }
            }
            case MONTH -> {
                YearMonth current = YearMonth.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy", Locale.getDefault());
                for (int i = periodType.window - 1; i >= 0; i--) {
                    YearMonth month = current.minusMonths(i);
                    slots.add(new PeriodSlot(month.toString(), month.format(formatter)));
                }
            }
            case QUARTER -> {
                int currentIndex = Year.now().getValue() * 4 + currentQuarterIndex(LocalDateTime.now()) - 1;
                for (int i = periodType.window - 1; i >= 0; i--) {
                    int index = currentIndex - i;
                    int year = index / 4;
                    int quarter = index % 4 + 1;
                    slots.add(new PeriodSlot(year + "-Q" + quarter, "Q" + quarter + "/" + year));
                }
            }
            case YEAR -> {
                int currentYear = Year.now().getValue();
                for (int i = periodType.window - 1; i >= 0; i--) {
                    int year = currentYear - i;
                    slots.add(new PeriodSlot(String.valueOf(year), String.valueOf(year)));
                }
            }
        }
        return slots;
    }

    private String periodKey(LocalDateTime dateTime, ReportPeriodType periodType) {
        switch (periodType) {
            case DAY -> {
                return String.valueOf(dateTime.getHour());
            }
            case MONTH -> {
                return YearMonth.from(dateTime).toString();
            }
            case QUARTER -> {
                int quarter = currentQuarterIndex(dateTime);
                return dateTime.getYear() + "-Q" + quarter;
            }
            case YEAR -> {
                return String.valueOf(dateTime.getYear());
            }
            default -> throw new IllegalStateException("Unsupported period type");
        }
    }

    private int currentQuarterIndex(LocalDateTime dateTime) {
        return ((dateTime.getMonthValue() - 1) / 3) + 1;
    }

    private List<TopProductRow> buildTopProducts(List<Order> deliveredOrders) {
        Map<Long, TopProductRow> byProductId = new LinkedHashMap<>();
        for (Order order : deliveredOrders) {
            if (order == null || order.getItems() == null) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (item == null || item.getVariant() == null) {
                    continue;
                }
                Product product = item.getVariant().getProduct();
                Long productId = product == null ? null : product.getProductId();
                if (productId == null) {
                    continue;
                }

                TopProductRow row = byProductId.computeIfAbsent(productId, id -> {
                    TopProductRow created = new TopProductRow();
                    created.setProductId(id);
                    created.setProductName(item.getProductName() != null ? item.getProductName()
                            : (product.getName() != null ? product.getName() : "Sản phẩm"));
                    created.setBrand(product.getBrand());
                    return created;
                });

                long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
                row.setQuantitySold(row.getQuantitySold() + quantity);
                row.setGrossRevenue(row.getGrossRevenue().add(safeBigDecimal(item.getSubtotal())));

                BigDecimal importPrice = safeBigDecimal(item.getVariant().getImportPrice());
                BigDecimal cost = importPrice.multiply(BigDecimal.valueOf(quantity));
                row.setEstimatedCost(row.getEstimatedCost().add(cost));
                row.setEstimatedProfit(row.getGrossRevenue().subtract(row.getEstimatedCost()));
            }
        }

        for (TopProductRow row : byProductId.values()) {
            row.setGrossProfitMargin(calculateMargin(row.getGrossRevenue(), row.getEstimatedProfit()));
        }

        return byProductId.values().stream()
                .sorted(Comparator.comparing(TopProductRow::getQuantitySold).reversed()
                        .thenComparing(TopProductRow::getGrossRevenue, Comparator.reverseOrder()))
                .limit(10)
                .toList();
    }

    private List<CategorySalesRow> buildCategorySales(List<Order> deliveredOrders) {
        Map<Long, CategorySalesRow> byCategoryId = new LinkedHashMap<>();
        for (Order order : deliveredOrders) {
            if (order == null || order.getItems() == null) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (item == null || item.getVariant() == null) {
                    continue;
                }
                Product product = item.getVariant().getProduct();
                if (product == null) {
                    continue;
                }
                Category category = product.getCategory();
                Long categoryId = category == null ? -1L : category.getCategoryId();
                String categoryName = category == null ? "Chưa phân loại" : category.getName();

                CategorySalesRow row = byCategoryId.computeIfAbsent(categoryId, id -> {
                    CategorySalesRow created = new CategorySalesRow();
                    created.setCategoryId(id);
                    created.setCategoryName(categoryName);
                    return created;
                });

                long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
                row.setQuantitySold(row.getQuantitySold() + quantity);
                row.setGrossRevenue(row.getGrossRevenue().add(safeBigDecimal(item.getSubtotal())));

                BigDecimal importPrice = safeBigDecimal(item.getVariant().getImportPrice());
                BigDecimal cost = importPrice.multiply(BigDecimal.valueOf(quantity));
                row.setEstimatedCost(row.getEstimatedCost().add(cost));
                row.setEstimatedProfit(row.getGrossRevenue().subtract(row.getEstimatedCost()));
            }
        }

        for (CategorySalesRow row : byCategoryId.values()) {
            row.setGrossProfitMargin(calculateMargin(row.getGrossRevenue(), row.getEstimatedProfit()));
        }

        return byCategoryId.values().stream()
                .sorted(Comparator.comparing(CategorySalesRow::getGrossRevenue).reversed())
                .toList();
    }

    private List<StockAlertRow> buildStockAlerts(List<ProductVariant> allVariants) {
        return allVariants.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((ProductVariant v) -> v.getStockQty() == null ? Integer.MAX_VALUE : v.getStockQty())
                        .thenComparing(v -> v.getSku() == null ? "" : v.getSku()))
                .filter(this::isLowStock)
                .map(variant -> {
                    StockAlertRow row = new StockAlertRow();
                    row.setVariantId(variant.getVariantId());
                    row.setSku(variant.getSku());
                    row.setStorageGb(variant.getStorageGb());
                    row.setStockQty(variant.getStockQty());
                    row.setPrice(safeBigDecimal(variant.getPrice()));
                    row.setImportPrice(safeBigDecimal(variant.getImportPrice()));
                    row.setInventoryValue(safeBigDecimal(variant.getImportPrice())
                            .multiply(BigDecimal.valueOf(variant.getStockQty() == null ? 0 : variant.getStockQty())));
                    row.setProductName(variant.getProduct() != null ? variant.getProduct().getName() : "—");
                    return row;
                })
                .limit(12)
                .toList();
    }

    private List<BrandSalesRow> buildBrandSales(List<Order> deliveredOrders) {
        Map<String, BrandSalesRow> byBrand = new LinkedHashMap<>();
        for (Order order : deliveredOrders) {
            if (order == null || order.getItems() == null) continue;
            for (OrderItem item : order.getItems()) {
                if (item == null || item.getVariant() == null || item.getVariant().getProduct() == null) continue;
                String brand = item.getVariant().getProduct().getBrand();
                if (brand == null || brand.isBlank()) brand = "Khác";

                BrandSalesRow row = byBrand.computeIfAbsent(brand, b -> {
                    BrandSalesRow created = new BrandSalesRow();
                    created.setBrand(b);
                    return created;
                });
                long qty = item.getQuantity() == null ? 0L : item.getQuantity();
                row.setQuantitySold(row.getQuantitySold() + qty);
                row.setGrossRevenue(row.getGrossRevenue().add(safeBigDecimal(item.getSubtotal())));
            }
        }
        return byBrand.values().stream()
                .sorted(Comparator.comparing(BrandSalesRow::getGrossRevenue).reversed())
                .toList();
    }

    private List<TopCustomerRow> buildTopCustomers(List<Order> deliveredOrders) {
        Map<Long, TopCustomerRow> byCustomer = new LinkedHashMap<>();
        for (Order order : deliveredOrders) {
            if (order == null || order.getCustomer() == null) continue;
            Customer c = order.getCustomer();
            Long cId = c.getUserID();
            if (cId == null) continue;
            
            TopCustomerRow row = byCustomer.computeIfAbsent(cId, id -> {
                TopCustomerRow created = new TopCustomerRow();
                created.setCustomerId(id);
                created.setCustomerName(c.getFullName());
                created.setCustomerPhone(c.getPhone() != null ? c.getPhone() : c.getEmail());
                return created;
            });
            row.setTotalOrders(row.getTotalOrders() + 1);
            row.setTotalSpent(row.getTotalSpent().add(safeBigDecimal(order.getTotalAmount())));
        }
        return byCustomer.values().stream()
                .sorted(Comparator.comparing(TopCustomerRow::getTotalSpent).reversed())
                .limit(10)
                .toList();
    }

    private boolean isLowStock(ProductVariant variant) {
        return variant != null
                && variant.getStockQty() != null
                && variant.getStockQty() <= LOW_STOCK_THRESHOLD;
    }

    private BigDecimal sumOrders(List<Order> orders) {
        return orders.stream()
                .map(order -> safeBigDecimal(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumDeliveryCost(List<Order> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            if (order == null || order.getItems() == null) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (item == null || item.getVariant() == null) {
                    continue;
                }
                BigDecimal importPrice = safeBigDecimal(item.getVariant().getImportPrice());
                long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
                total = total.add(importPrice.multiply(BigDecimal.valueOf(quantity)));
            }
        }
        return total;
    }

    private BigDecimal sumInventoryValue(List<ProductVariant> variants) {
        BigDecimal total = BigDecimal.ZERO;
        for (ProductVariant variant : variants) {
            if (variant == null) {
                continue;
            }
            BigDecimal importPrice = safeBigDecimal(variant.getImportPrice());
            long stock = variant.getStockQty() == null ? 0L : variant.getStockQty();
            total = total.add(importPrice.multiply(BigDecimal.valueOf(stock)));
        }
        return total;
    }

    private BigDecimal calculateOrderProfit(Order order) {
        if (order == null || order.getItems() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal revenue = safeBigDecimal(order.getTotalAmount());
        BigDecimal cost = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getVariant() == null) {
                continue;
            }
            BigDecimal importPrice = safeBigDecimal(item.getVariant().getImportPrice());
            long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
            cost = cost.add(importPrice.multiply(BigDecimal.valueOf(quantity)));
        }
        return revenue.subtract(cost);
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void writeSummarySheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Summary");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle percentStyle = createPercentStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);

        int rowNum = 0;

        Row title = sheet.createRow(rowNum++);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("BÁO CÁO KINH DOANH PHONESHOP");
        titleCell.setCellStyle(titleStyle);
        title.createCell(1).setCellValue(view.getPeriodLabel() == null ? "Tháng này" : view.getPeriodLabel());
        title.getCell(1).setCellStyle(titleStyle);

        rowNum++;
        
        Row headerRow = sheet.createRow(rowNum++);
        Cell h1 = headerRow.createCell(0); h1.setCellValue("Chỉ số"); h1.setCellStyle(headerStyle);
        Cell h2 = headerRow.createCell(1); h2.setCellValue("Giá trị"); h2.setCellStyle(headerStyle);

        rowNum = writeKeyValue(sheet, rowNum, "Tổng đơn hàng", view.getSummary().getTotalOrders(), dataStyle);
        rowNum = writeKeyValue(sheet, rowNum, "Đơn đã giao", view.getSummary().getDeliveredOrders(), dataStyle);
        rowNum = writeKeyValue(sheet, rowNum, "Đơn đã hủy", view.getSummary().getCancelledOrders(), dataStyle);
        rowNum = writeKeyValue(sheet, rowNum, "Sản phẩm active", view.getSummary().getActiveProducts(), dataStyle);
        rowNum = writeKeyValue(sheet, rowNum, "Biến thể", view.getSummary().getTotalVariants(), dataStyle);
        rowNum = writeKeyValue(sheet, rowNum, "Biến thể tồn thấp", view.getSummary().getLowStockVariants(), dataStyle);
        rowNum = writeKeyValue(sheet, rowNum, "Tổng tồn kho", view.getSummary().getTotalStockQty(), dataStyle);
        rowNum = writeKeyCurrency(sheet, rowNum, "Doanh số", view.getSummary().getGrossRevenue(), currencyStyle);
        rowNum = writeKeyCurrency(sheet, rowNum, "Doanh thu thực nhận", view.getSummary().getRealizedRevenue(), currencyStyle);
        rowNum = writeKeyCurrency(sheet, rowNum, "Chi phí ước tính", view.getSummary().getEstimatedCost(), currencyStyle);
        rowNum = writeKeyCurrency(sheet, rowNum, "Lợi nhuận ước tính", view.getSummary().getEstimatedProfit(), currencyStyle);
        rowNum = writeKeyPercent(sheet, rowNum, "Biên lợi nhuận gộp", view.getSummary().getGrossProfitMargin(), percentStyle);
        writeKeyCurrency(sheet, rowNum, "Giá trị tồn kho", view.getSummary().getInventoryValue(), currencyStyle);

        autoSize(sheet, 2);
    }

    private void writePeriodSheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Period");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        createTableHeader(sheet, headerStyle, "Kỳ", "Đơn hàng", "Doanh số", "Doanh thu thực nhận", "Lợi nhuận ước tính");
        int rowNum = 1;
        for (ReportPeriodPoint point : view.getPeriodPoints()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, point.getLabel(), dataStyle);
            createCell(row, 1, point.getOrders(), dataStyle);
            createCell(row, 2, point.getGrossRevenue().doubleValue(), currencyStyle);
            createCell(row, 3, point.getRealizedRevenue().doubleValue(), currencyStyle);
            createCell(row, 4, point.getEstimatedProfit().doubleValue(), currencyStyle);
        }
        
        // Add Total Row
        if (rowNum > 1) {
            Row totalRow = sheet.createRow(rowNum);
            createCell(totalRow, 0, "TỔNG CỘNG", headerStyle);
            createFormulaCell(totalRow, 1, "SUM(B2:B" + rowNum + ")", dataStyle);
            createFormulaCell(totalRow, 2, "SUM(C2:C" + rowNum + ")", currencyStyle);
            createFormulaCell(totalRow, 3, "SUM(D2:D" + rowNum + ")", currencyStyle);
            createFormulaCell(totalRow, 4, "SUM(E2:E" + rowNum + ")", currencyStyle);
        }
        
        autoSize(sheet, 5);
    }

    private void writeTopProductsSheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Top Products");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle percentStyle = createPercentStyle(workbook);

        createTableHeader(sheet, headerStyle, "Sản phẩm", "Hãng", "SL bán", "Doanh số", "Chi phí", "Lợi nhuận", "Biên lợi nhuận");
        int rowNum = 1;
        for (TopProductRow rowData : view.getTopProducts()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, rowData.getProductName(), dataStyle);
            createCell(row, 1, rowData.getBrand() == null ? "" : rowData.getBrand(), dataStyle);
            createCell(row, 2, rowData.getQuantitySold(), dataStyle);
            createCell(row, 3, rowData.getGrossRevenue().doubleValue(), currencyStyle);
            createCell(row, 4, rowData.getEstimatedCost().doubleValue(), currencyStyle);
            createCell(row, 5, rowData.getEstimatedProfit().doubleValue(), currencyStyle);
            createCell(row, 6, rowData.getGrossProfitMargin().doubleValue(), percentStyle);
        }
        autoSize(sheet, 7);
    }

    private void writeCategorySalesSheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Sales by Category");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle percentStyle = createPercentStyle(workbook);

        createTableHeader(sheet, headerStyle, "Danh mục", "SL bán", "Doanh số", "Chi phí", "Lợi nhuận", "Biên lợi nhuận");
        int rowNum = 1;
        for (CategorySalesRow rowData : view.getCategorySales()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, rowData.getCategoryName(), dataStyle);
            createCell(row, 1, rowData.getQuantitySold(), dataStyle);
            createCell(row, 2, rowData.getGrossRevenue().doubleValue(), currencyStyle);
            createCell(row, 3, rowData.getEstimatedCost().doubleValue(), currencyStyle);
            createCell(row, 4, rowData.getEstimatedProfit().doubleValue(), currencyStyle);
            createCell(row, 5, rowData.getGrossProfitMargin().doubleValue(), percentStyle);
        }
        
        if (rowNum > 1) {
            Row totalRow = sheet.createRow(rowNum);
            createCell(totalRow, 0, "TỔNG CỘNG", headerStyle);
            createFormulaCell(totalRow, 1, "SUM(B2:B" + rowNum + ")", dataStyle);
            createFormulaCell(totalRow, 2, "SUM(C2:C" + rowNum + ")", currencyStyle);
            createFormulaCell(totalRow, 3, "SUM(D2:D" + rowNum + ")", currencyStyle);
            createFormulaCell(totalRow, 4, "SUM(E2:E" + rowNum + ")", currencyStyle);
        }

        autoSize(sheet, 6);
    }

    private void writeStockAlertsSheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Stock Alerts");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle alertStyle = createAlertDataStyle(workbook);

        createTableHeader(sheet, headerStyle, "Sản phẩm", "SKU", "Dung lượng", "Tồn kho", "Giá", "Giá nhập", "Giá trị tồn");
        int rowNum = 1;
        for (StockAlertRow rowData : view.getStockAlerts()) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, rowData.getProductName(), dataStyle);
            createCell(row, 1, rowData.getSku() == null ? "" : rowData.getSku(), dataStyle);
            createCell(row, 2, rowData.getStorageGb() == null ? "" : rowData.getStorageGb() + " GB", dataStyle);
            createCell(row, 3, rowData.getStockQty() == null ? 0 : rowData.getStockQty(), alertStyle);
            createCell(row, 4, rowData.getPrice().doubleValue(), currencyStyle);
            createCell(row, 5, rowData.getImportPrice().doubleValue(), currencyStyle);
            createCell(row, 6, rowData.getInventoryValue().doubleValue(), currencyStyle);
        }
        autoSize(sheet, 7);
    }

    private void createTableHeader(Sheet sheet, CellStyle headerStyle, String... headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private int writeKeyValue(Sheet sheet, int rowNum, String label, Object value, CellStyle dataStyle) {
        Row row = sheet.createRow(rowNum++);
        createCell(row, 0, label, dataStyle);
        if (value instanceof Number num) {
            createCell(row, 1, num.doubleValue(), dataStyle);
        } else {
            createCell(row, 1, value.toString(), dataStyle);
        }
        return rowNum;
    }

    private int writeKeyCurrency(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle currencyStyle) {
        Row row = sheet.createRow(rowNum++);
        createCell(row, 0, label, createDataStyle(sheet.getWorkbook()));
        createCell(row, 1, value.doubleValue(), currencyStyle);
        return rowNum;
    }

    private int writeKeyPercent(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle percentStyle) {
        Row row = sheet.createRow(rowNum++);
        createCell(row, 0, label, createDataStyle(sheet.getWorkbook()));
        createCell(row, 1, value.doubleValue(), percentStyle);
        return rowNum;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createFormulaCell(Row row, int col, String formula, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellFormula(formula);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        style.setFont(font);
        
        setBorders(style);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorders(style);
        return style;
    }

    private CellStyle createAlertDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorders(style);
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorders(style);
        org.apache.poi.ss.usermodel.DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        setBorders(style);
        org.apache.poi.ss.usermodel.DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        return style;
    }

    private void setBorders(CellStyle style) {
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String formatMoney(BigDecimal value) {
        return safeBigDecimal(value).toPlainString();
    }

    private enum ReportPeriodType {
        DAY("day", "Báo cáo theo giờ", 24),
        MONTH("month", "Báo cáo theo tháng", 6),
        QUARTER("quarter", "Báo cáo theo quý", 6),
        YEAR("year", "Báo cáo theo năm", 5);

        private final String code;
        private final String displayLabel;
        private final int window;

        ReportPeriodType(String code, String displayLabel, int window) {
            this.code = code;
            this.displayLabel = displayLabel;
            this.window = window;
        }

        private static ReportPeriodType from(String raw) {
            if (raw == null || raw.isBlank()) {
                return MONTH;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "quarter", "quý", "qui" -> QUARTER;
                case "year", "năm", "nam" -> YEAR;
                default -> MONTH;
            };
        }
    }

    private record PeriodSlot(String key, String label) {}
}
