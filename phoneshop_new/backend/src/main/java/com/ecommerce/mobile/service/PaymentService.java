package com.ecommerce.mobile.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.Payment;
import com.ecommerce.mobile.enums.PaymentMethod;
import com.ecommerce.mobile.enums.PaymentStatus;
import com.ecommerce.mobile.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createPayment(Order order, PaymentMethod method) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method == null ? PaymentMethod.COD : method);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionRef(generateTransactionRef(payment.getMethod()));
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentForCustomer(Long paymentId, String customerEmail) {
        return paymentRepository.findDetailedByPaymentId(paymentId)
                .filter(payment -> payment.getOrder() != null
                        && payment.getOrder().getCustomer() != null
                        && customerEmail.equals(payment.getOrder().getCustomer().getEmail()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Payment getLatestPaymentForOrder(Long orderId) {
        return paymentRepository.findFirstByOrderOrderIdOrderByCreatedAtDesc(orderId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForOrder(Long orderId) {
        return paymentRepository.findByOrderOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Transactional
    public Payment confirmPayment(Long paymentId, String customerEmail) {
        Payment payment = getPaymentForCustomer(paymentId, customerEmail);
        if (payment == null) {
            throw new RuntimeException("Không tìm thấy thanh toán");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }
        if (payment.getOrder() != null && payment.getOrder().getStatus() == com.ecommerce.mobile.enums.OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng đã hủy, không thể xác nhận thanh toán");
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        if (payment.getOrder() != null) {
            payment.getOrder().setStatus(com.ecommerce.mobile.enums.OrderStatus.CONFIRMED);
        }
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByTransactionRef(String transactionRef) {
        return paymentRepository.findByTransactionRef(transactionRef).orElse(null);
    }

    private String generateTransactionRef(PaymentMethod method) {
        return method.name() + "-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
