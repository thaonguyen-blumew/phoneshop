package com.ecommerce.mobile.enums;

public enum ShipmentStatus {
    PENDING,      // Chờ lấy hàng / chờ GHN xác nhận
    PICKED_UP,    // Shipper đã lấy hàng
    IN_TRANSIT,   // Đang trên đường vận chuyển
    DELIVERED,    // Đã giao tới tay khách
    CANCELLED,    // Đơn bị hủy
    RETURNING,    // Đang trả hàng
    RETURNED,     // Đã trả về shop
    EXCEPTION     // Sự cố trong quá trình giao
}
