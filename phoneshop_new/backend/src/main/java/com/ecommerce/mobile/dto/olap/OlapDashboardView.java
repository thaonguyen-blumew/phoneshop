package com.ecommerce.mobile.dto.olap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OlapDashboardView {

    private boolean available = true;
    private String message = "";
    private String grain = "day";
    private String periodLabel = "";
    private LocalDate startDate;
    private LocalDate endDate;
    private Summary summary = new Summary();
    private List<TrendPoint> salesTrend = new ArrayList<>();
    private List<ProductPerformance> topProducts = new ArrayList<>();
    private List<ProductPerformance> slowMovingProducts = new ArrayList<>();
    private List<BreakdownPoint> revenueByBrand = new ArrayList<>();
    private List<BreakdownPoint> revenueByCategory = new ArrayList<>();
    private List<BreakdownPoint> revenueByCustomerSegment = new ArrayList<>();
    private List<BreakdownPoint> inventoryByCategory = new ArrayList<>();
    private List<BreakdownPoint> paymentReconciliation = new ArrayList<>();
    private List<BreakdownPoint> ratingDistribution = new ArrayList<>();
    private List<ReviewWarning> reviewWarnings = new ArrayList<>();
    private List<MismatchedPayment> mismatchedPayments = new ArrayList<>();

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getGrain() {
        return grain;
    }

    public void setGrain(String grain) {
        this.grain = grain;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public List<TrendPoint> getSalesTrend() {
        return salesTrend;
    }

    public void setSalesTrend(List<TrendPoint> salesTrend) {
        this.salesTrend = salesTrend;
    }

    public List<ProductPerformance> getTopProducts() {
        return topProducts;
    }

    public void setTopProducts(List<ProductPerformance> topProducts) {
        this.topProducts = topProducts;
    }

    public List<ProductPerformance> getSlowMovingProducts() {
        return slowMovingProducts;
    }

    public void setSlowMovingProducts(List<ProductPerformance> slowMovingProducts) {
        this.slowMovingProducts = slowMovingProducts;
    }

    public List<BreakdownPoint> getRevenueByBrand() {
        return revenueByBrand;
    }

    public void setRevenueByBrand(List<BreakdownPoint> revenueByBrand) {
        this.revenueByBrand = revenueByBrand;
    }

    public List<BreakdownPoint> getRevenueByCategory() {
        return revenueByCategory;
    }

    public void setRevenueByCategory(List<BreakdownPoint> revenueByCategory) {
        this.revenueByCategory = revenueByCategory;
    }

    public List<BreakdownPoint> getRevenueByCustomerSegment() {
        return revenueByCustomerSegment;
    }

    public void setRevenueByCustomerSegment(List<BreakdownPoint> revenueByCustomerSegment) {
        this.revenueByCustomerSegment = revenueByCustomerSegment;
    }

    public List<BreakdownPoint> getInventoryByCategory() {
        return inventoryByCategory;
    }

    public void setInventoryByCategory(List<BreakdownPoint> inventoryByCategory) {
        this.inventoryByCategory = inventoryByCategory;
    }

    public List<BreakdownPoint> getPaymentReconciliation() {
        return paymentReconciliation;
    }

    public void setPaymentReconciliation(List<BreakdownPoint> paymentReconciliation) {
        this.paymentReconciliation = paymentReconciliation;
    }

    public List<BreakdownPoint> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(List<BreakdownPoint> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    public List<ReviewWarning> getReviewWarnings() {
        return reviewWarnings;
    }

    public void setReviewWarnings(List<ReviewWarning> reviewWarnings) {
        this.reviewWarnings = reviewWarnings;
    }

    public static class Summary {
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal grossProfit = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;
        private BigDecimal grossMarginRate = BigDecimal.ZERO;
        private BigDecimal netMarginRate = BigDecimal.ZERO;
        private long unitsSold;
        private long orderCount;
        private BigDecimal inventoryValue = BigDecimal.ZERO;
        private long inventoryQuantity;
        private BigDecimal netReceived = BigDecimal.ZERO;
        private BigDecimal paymentMatchRate = BigDecimal.ZERO;
        private BigDecimal averageRating = BigDecimal.ZERO;
        private BigDecimal verifiedReviewRate = BigDecimal.ZERO;
        private long lowStockProducts;
        private long slowMovingProducts;

        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getGrossProfit() { return grossProfit; }
        public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
        public BigDecimal getNetProfit() { return netProfit; }
        public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
        public BigDecimal getGrossMarginRate() { return grossMarginRate; }
        public void setGrossMarginRate(BigDecimal grossMarginRate) { this.grossMarginRate = grossMarginRate; }
        public BigDecimal getNetMarginRate() { return netMarginRate; }
        public void setNetMarginRate(BigDecimal netMarginRate) { this.netMarginRate = netMarginRate; }
        public long getUnitsSold() { return unitsSold; }
        public void setUnitsSold(long unitsSold) { this.unitsSold = unitsSold; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getInventoryValue() { return inventoryValue; }
        public void setInventoryValue(BigDecimal inventoryValue) { this.inventoryValue = inventoryValue; }
        public long getInventoryQuantity() { return inventoryQuantity; }
        public void setInventoryQuantity(long inventoryQuantity) { this.inventoryQuantity = inventoryQuantity; }
        public BigDecimal getNetReceived() { return netReceived; }
        public void setNetReceived(BigDecimal netReceived) { this.netReceived = netReceived; }
        public BigDecimal getPaymentMatchRate() { return paymentMatchRate; }
        public void setPaymentMatchRate(BigDecimal paymentMatchRate) { this.paymentMatchRate = paymentMatchRate; }
        public BigDecimal getAverageRating() { return averageRating; }
        public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }
        public BigDecimal getVerifiedReviewRate() { return verifiedReviewRate; }
        public void setVerifiedReviewRate(BigDecimal verifiedReviewRate) { this.verifiedReviewRate = verifiedReviewRate; }
        public long getLowStockProducts() { return lowStockProducts; }
        public void setLowStockProducts(long lowStockProducts) { this.lowStockProducts = lowStockProducts; }
        public long getSlowMovingProducts() { return slowMovingProducts; }
        public void setSlowMovingProducts(long slowMovingProducts) { this.slowMovingProducts = slowMovingProducts; }
    }

    public static class TrendPoint {
        private String label;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal grossProfit = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;
        private long unitsSold;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getGrossProfit() { return grossProfit; }
        public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
        public BigDecimal getNetProfit() { return netProfit; }
        public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
        public long getUnitsSold() { return unitsSold; }
        public void setUnitsSold(long unitsSold) { this.unitsSold = unitsSold; }
    }

    public static class BreakdownPoint {
        private String label;
        private BigDecimal value = BigDecimal.ZERO;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
    }

    public static class ProductPerformance {
        private String productName;
        private String brand;
        private String categoryName;
        private long unitsSold;
        private long quantityOnHand;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal netProfit = BigDecimal.ZERO;
        private BigDecimal inventoryValue = BigDecimal.ZERO;
        private BigDecimal slowMovingScore = BigDecimal.ZERO;
        private String suggestedAction = "Theo dõi";

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public long getUnitsSold() { return unitsSold; }
        public void setUnitsSold(long unitsSold) { this.unitsSold = unitsSold; }
        public long getQuantityOnHand() { return quantityOnHand; }
        public void setQuantityOnHand(long quantityOnHand) { this.quantityOnHand = quantityOnHand; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getNetProfit() { return netProfit; }
        public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
        public BigDecimal getInventoryValue() { return inventoryValue; }
        public void setInventoryValue(BigDecimal inventoryValue) { this.inventoryValue = inventoryValue; }
        public BigDecimal getSlowMovingScore() { return slowMovingScore; }
        public void setSlowMovingScore(BigDecimal slowMovingScore) { this.slowMovingScore = slowMovingScore; }
        public String getSuggestedAction() { return suggestedAction; }
        public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    }

    public static class ReviewWarning {
        private String productName;
        private String brand;
        private long reviewCount;
        private BigDecimal averageRating = BigDecimal.ZERO;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public long getReviewCount() { return reviewCount; }
        public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
        public BigDecimal getAverageRating() { return averageRating; }
        public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }
    }

    public List<MismatchedPayment> getMismatchedPayments() {
        return mismatchedPayments;
    }

    public void setMismatchedPayments(List<MismatchedPayment> mismatchedPayments) {
        this.mismatchedPayments = mismatchedPayments;
    }

    public static class MismatchedPayment {
        private Long orderId;
        private java.time.LocalDateTime paymentTime;
        private String customerName;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal gatewayFee = BigDecimal.ZERO;
        private BigDecimal netReceived = BigDecimal.ZERO;
        private String reconciliationStatus;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public java.time.LocalDateTime getPaymentTime() { return paymentTime; }
        public void setPaymentTime(java.time.LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getGatewayFee() { return gatewayFee; }
        public void setGatewayFee(BigDecimal gatewayFee) { this.gatewayFee = gatewayFee; }
        public BigDecimal getNetReceived() { return netReceived; }
        public void setNetReceived(BigDecimal netReceived) { this.netReceived = netReceived; }
        public String getReconciliationStatus() { return reconciliationStatus; }
        public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    }
}
