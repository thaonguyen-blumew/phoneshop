package com.ecommerce.mobile.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.dto.response.PaymentDto;
import com.ecommerce.mobile.entity.Payment;
import com.ecommerce.mobile.enums.PaymentMethod;
import com.ecommerce.mobile.enums.PaymentStatus;
import com.ecommerce.mobile.mapper.OrderMapper;
import com.ecommerce.mobile.mapper.PaymentMapper;
import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.service.PaymentService;
import com.ecommerce.mobile.service.VnpayService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final VnpayService vnpayService;

    public PaymentController(PaymentService paymentService, VnpayService vnpayService) {
        this.paymentService = paymentService;
        this.vnpayService = vnpayService;
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentDto> paymentDetail(@AuthenticationPrincipal UserDetails principal,
                                                 @PathVariable Long paymentId) {
        Payment payment = paymentService.getPaymentForCustomer(paymentId, principal.getUsername());
        if (payment == null) {
            return ApiResponse.error(404, "Không tìm thấy thanh toán");
        }
        return ApiResponse.success("Lấy thông tin thanh toán thành công", PaymentMapper.toDto(payment));
    }

    @GetMapping("/{paymentId}/vnpay-url")
    public ApiResponse<Map<String, Object>> getVnpayUrl(@AuthenticationPrincipal UserDetails principal,
                                                        @PathVariable Long paymentId,
                                                        HttpServletRequest request) {
        Payment payment = paymentService.getPaymentForCustomer(paymentId, principal.getUsername());
        if (payment == null) {
            return ApiResponse.error(404, "Không tìm thấy thanh toán");
        }
        if (payment.getMethod() != PaymentMethod.VN_PAY) {
            return ApiResponse.error(400, "Thanh toán này không dùng VNPAY");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return ApiResponse.error(400, "Thanh toán đã hoàn tất");
        }
        String paymentUrl = vnpayService.createPaymentUrl(payment, request);
        Map<String, Object> data = new HashMap<>();
        data.put("paymentUrl", paymentUrl);
        data.put("paymentId", paymentId);
        return ApiResponse.success("Tạo URL thanh toán VNPAY thành công", data);
    }

    @GetMapping("/{paymentId}/vnpay/mock")
    public ApiResponse<Map<String, Object>> mockGateway(@AuthenticationPrincipal UserDetails principal,
                                                        @PathVariable Long paymentId) {
        Payment payment = paymentService.getPaymentForCustomer(paymentId, principal.getUsername());
        if (payment == null) {
            return ApiResponse.error(404, "Không tìm thấy thanh toán");
        }
        if (payment.getMethod() != PaymentMethod.VN_PAY) {
            return ApiResponse.error(400, "Thanh toán này không dùng VNPAY");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("payment", PaymentMapper.toDto(payment));
        data.put("orderId", payment.getOrder() != null ? payment.getOrder().getOrderId() : null);
        data.put("orderCode", payment.getOrder() != null ? payment.getOrder().getOrderCode() : null);
        data.put("totalAmount", payment.getOrder() != null ? payment.getOrder().getTotalAmount() : null);
        return ApiResponse.success("Lấy thông tin mock VNPAY thành công", data);
    }

    @PostMapping("/{paymentId}/vnpay/mock/confirm")
    public ApiResponse<PaymentDto> mockConfirm(@AuthenticationPrincipal UserDetails principal,
                                               @PathVariable Long paymentId) {
        Payment payment = vnpayService.simulateLocalResult(paymentId, principal.getUsername(), true);
        return ApiResponse.success("Đã mô phỏng thanh toán VNPAY thành công", PaymentMapper.toDto(payment));
    }

    @PostMapping("/{paymentId}/vnpay/mock/fail")
    public ApiResponse<PaymentDto> mockFail(@AuthenticationPrincipal UserDetails principal,
                                            @PathVariable Long paymentId) {
        Payment payment = vnpayService.simulateLocalResult(paymentId, principal.getUsername(), false);
        return ApiResponse.success("Đã mô phỏng thanh toán VNPAY thất bại", PaymentMapper.toDto(payment));
    }

    @PostMapping("/{paymentId}/confirm")
    public ApiResponse<PaymentDto> confirmPayment(@AuthenticationPrincipal UserDetails principal,
                                                  @PathVariable Long paymentId) {
        Payment payment = paymentService.confirmPayment(paymentId, principal.getUsername());
        return ApiResponse.success("Đã xác nhận thanh toán thành công", PaymentMapper.toDto(payment));
    }

    @GetMapping("/vnpay/return")
    public void vnpayReturn(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        VnpayService.CallbackResult result = vnpayService.processCallback(request, true);
        if (result.ok() && result.payment() != null) {
            response.sendRedirect("/orders/" + result.payment().getOrder().getOrderId() + "?payment=success");
        } else {
            response.sendRedirect("/orders");
        }
    }

    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnpayIpn(HttpServletRequest request) {
        VnpayService.CallbackResult result = vnpayService.processCallback(request, true);
        return vnpayService.buildIpnResponse(result);
    }
}
