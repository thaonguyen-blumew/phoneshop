package com.ecommerce.mobile.enums;

public enum OrderStatus {
    PENDING,    // Chờ xác nhận — trạng thái mới đặt
    CONFIRMED,  // Đã xác nhận — nhân viên đã duyệt
    PACKING,    // Đang đóng gói — kho đang chuẩn bị
    SHIPPING,   // Đang giao — shipper đang giao
    DELIVERED,  // Đã giao — khách đã nhận
    CANCELLED   // Đã hủy — khách hoặc shop hủy

}
