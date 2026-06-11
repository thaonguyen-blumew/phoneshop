package com.ecommerce.mobile.mapper;

import com.ecommerce.mobile.dto.response.PaymentDto;
import com.ecommerce.mobile.entity.Payment;

public class PaymentMapper {

    public static PaymentDto toDto(Payment payment) {
        if (payment == null) return null;

        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .method(payment.getMethod() != null ? payment.getMethod().name() : null)
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .transactionRef(payment.getTransactionRef())
                .bankCode(payment.getBankCode())
                .cardType(payment.getCardType())
                .responseCode(payment.getResponseCode())
                .responseMessage(payment.getResponseMessage())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .orderId(payment.getOrder() != null ? payment.getOrder().getOrderId() : null)
                .orderCode(payment.getOrder() != null ? payment.getOrder().getOrderCode() : null)
                .build();
    }
}
