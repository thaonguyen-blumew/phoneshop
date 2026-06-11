package com.ecommerce.mobile.dto.response;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentDto {
    private Long paymentId;
    private String method;
    private String status;
    private String transactionRef;
    private String bankCode;
    private String cardType;
    private String responseCode;
    private String responseMessage;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private Long orderId;
    private String orderCode;
}
