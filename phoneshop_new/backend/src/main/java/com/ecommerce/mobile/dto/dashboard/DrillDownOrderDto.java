package com.ecommerce.mobile.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DrillDownOrderDto {
    private Long orderId;
    private String customerName;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
