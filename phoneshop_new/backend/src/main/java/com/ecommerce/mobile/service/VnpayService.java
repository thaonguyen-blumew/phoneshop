package com.ecommerce.mobile.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.HashMap;

import com.ecommerce.mobile.config.VnpayProperties;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.Payment;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.enums.PaymentStatus;
import com.ecommerce.mobile.repository.OrderRepository;
import com.ecommerce.mobile.repository.PaymentRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class VnpayService {

    private static final ZoneId HCMC = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties properties;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public VnpayService(VnpayProperties properties,
                        PaymentRepository paymentRepository,
                        OrderRepository orderRepository) {
        this.properties = properties;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public String createPaymentUrl(Payment payment, HttpServletRequest request) {
        if (payment == null || payment.getOrder() == null) {
            throw new RuntimeException("Không tìm thấy dữ liệu thanh toán");
        }
        if (properties.isMockMode()) {
            return "/api/payments/" + payment.getPaymentId() + "/vnpay/mock";
        }
        validateConfig();

        Order order = payment.getOrder();
        BigDecimal amount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        long vnpAmount = amount.movePointRight(2).longValueExact();

        Map<String, String> fields = new TreeMap<>();
        fields.put("vnp_Amount", String.valueOf(vnpAmount));
        fields.put("vnp_Command", "pay");
        fields.put("vnp_CreateDate", VNPAY_DATE_FORMAT.format(ZonedDateTime.now(HCMC)));
        fields.put("vnp_CurrCode", properties.getCurrencyCode());
        fields.put("vnp_IpAddr", getClientIpAddress(request));
        fields.put("vnp_Locale", StringUtils.hasText(properties.getLocale()) ? properties.getLocale() : "vn");
        fields.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null) {
            scheme = request.getScheme();
        }
        String host = request.getHeader("Host");
        String dynamicReturnUrl = scheme + "://" + host + "/api/payments/vnpay/return";

        fields.put("vnp_OrderType", StringUtils.hasText(properties.getOrderType()) ? properties.getOrderType() : "other");
        fields.put("vnp_ReturnUrl", dynamicReturnUrl);
        fields.put("vnp_TmnCode", properties.getTmnCode());
        fields.put("vnp_TxnRef", payment.getTransactionRef());
        fields.put("vnp_Version", StringUtils.hasText(properties.getVersion()) ? properties.getVersion() : "2.1.0");
        fields.put("vnp_ExpireDate",
                VNPAY_DATE_FORMAT.format(ZonedDateTime.now(HCMC).plusMinutes(Math.max(properties.getExpireMinutes(), 1))));

        String hashData = buildHashData(fields);
        String secureHash = hmacSHA512(properties.getHashSecret(), hashData);
        return properties.getPaymentUrl() + "?" + buildQueryString(fields) + "&vnp_SecureHash=" + secureHash;
    }

    @Transactional
    public CallbackResult processCallback(HttpServletRequest request, boolean updateDatabase) {
        validateConfig();

        Map<String, String> fields = extractFields(request);
        if (fields.isEmpty()) {
            return CallbackResult.error("99", "Invalid request");
        }

        String secureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        String signValue = hmacSHA512(properties.getHashSecret(), buildHashData(fields));
        if (!signValue.equalsIgnoreCase(secureHash)) {
            return CallbackResult.error("97", "Invalid signature");
        }

        String txnRef = fields.get("vnp_TxnRef");
        if (!StringUtils.hasText(txnRef)) {
            return CallbackResult.error("01", "Order not found");
        }

        Optional<Payment> optionalPayment = paymentRepository.findDetailedByTransactionRef(txnRef);
        if (optionalPayment.isEmpty()) {
            return CallbackResult.error("01", "Order not found");
        }

        Payment payment = optionalPayment.get();
        Order order = payment.getOrder();
        long expectedAmount = order.getTotalAmount() == null
                ? 0L
                : order.getTotalAmount().movePointRight(2).longValueExact();
        long returnedAmount = parseLong(fields.get("vnp_Amount"));
        if (expectedAmount != returnedAmount) {
            return CallbackResult.error("04", "Invalid Amount");
        }

        boolean alreadyProcessed = payment.getStatus() == PaymentStatus.SUCCESS;
        if (alreadyProcessed) {
            return CallbackResult.error("02", "Order already confirmed");
        }

        String responseCode = fields.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = fields.getOrDefault("vnp_TransactionStatus", "");
        String bankCode = fields.get("vnp_BankCode");
        String cardType = fields.get("vnp_CardType");
        String bankTranNo = fields.get("vnp_BankTranNo");
        String transactionNo = fields.get("vnp_TransactionNo");
        String payDate = fields.get("vnp_PayDate");

        if (updateDatabase) {
            payment.setBankCode(bankCode);
            payment.setCardType(cardType);
            payment.setBankTransactionNo(bankTranNo);
            payment.setGatewayTransactionNo(transactionNo);
            payment.setResponseCode(responseCode);
            payment.setTransactionStatus(transactionStatus);
            payment.setPayDate(payDate);
            payment.setResponseMessage("00".equals(responseCode) && "00".equals(transactionStatus)
                    ? "Thanh toán thành công"
                    : "Thanh toán không thành công");

            if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaidAt(ZonedDateTime.now(HCMC).toLocalDateTime());
                if (order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.CONFIRMED);
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }

            paymentRepository.save(payment);
            orderRepository.save(order);
        }

        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            return CallbackResult.success("00", "Confirm Success", payment);
        }
        return CallbackResult.error(responseCode.isBlank() ? "01" : responseCode, "Payment Failed");
    }

    @Transactional
    public Payment simulateLocalResult(Long paymentId, String customerEmail, boolean success) {
        Payment payment = paymentRepository.findDetailedByPaymentId(paymentId)
                .filter(p -> p.getOrder() != null
                        && p.getOrder().getCustomer() != null
                        && customerEmail.equals(p.getOrder().getCustomer().getEmail()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thanh toán"));

        if (payment.getMethod() != com.ecommerce.mobile.enums.PaymentMethod.VN_PAY) {
            throw new RuntimeException("Thanh toán này không dùng VNPAY");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }

        if (success) {
            payment.setGatewayTransactionNo("MOCK-" + payment.getTransactionRef());
            payment.setBankTransactionNo("MOCK-" + payment.getTransactionRef());
            payment.setBankCode("MOCK");
            payment.setCardType("QR");
            payment.setResponseCode("00");
            payment.setTransactionStatus("00");
            payment.setPayDate(DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(ZonedDateTime.now(HCMC)));
            payment.setResponseMessage("Thanh toán thành công (mô phỏng local)");
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.PENDING) {
                payment.getOrder().setStatus(OrderStatus.CONFIRMED);
            }
        } else {
            payment.setGatewayTransactionNo("MOCK-" + payment.getTransactionRef());
            payment.setBankTransactionNo(null);
            payment.setBankCode("MOCK");
            payment.setCardType("QR");
            payment.setResponseCode("01");
            payment.setTransactionStatus("01");
            payment.setPayDate(DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(ZonedDateTime.now(HCMC)));
            payment.setResponseMessage("Thanh toán thất bại (mô phỏng local)");
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);
        if (payment.getOrder() != null) {
            orderRepository.save(payment.getOrder());
        }
        return payment;
    }

    public Map<String, String> buildIpnResponse(CallbackResult callbackResult) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("RspCode", callbackResult.rspCode());
        response.put("Message", callbackResult.message());
        return response;
    }

    private Map<String, String> extractFields(HttpServletRequest request) {
        Map<String, String> fields = new TreeMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key != null && key.startsWith("vnp_") && values != null && values.length > 0 && values[0] != null) {
                fields.put(key, values[0]);
            }
        });
        return fields;
    }

    private String buildQueryString(Map<String, String> fields) {
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!first) {
                query.append('&');
            }
            first = false;
            query.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(entry.getValue()));
        }
        return query.toString();
    }

    private String buildHashData(Map<String, String> fields) {
        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!first) {
                hashData.append('&');
            }
            first = false;
            // VNPAY standard does NOT encode the key in hashData
            hashData.append(entry.getKey())
                    .append('=')
                    .append(urlEncode(entry.getValue()));
        }
        return hashData.toString();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
        } catch (Exception e) {
            return value;
        }
    }

    private String hmacSHA512(String secretKey, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Không tạo được chữ ký VNPAY", ex);
        }
    }

    private long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public void refundPayment(Payment payment, String cancelReason, String createBy, String ipAddr) {
        if (properties.isMockMode()) {
            return;
        }
        
        String vnp_RequestId = java.util.UUID.randomUUID().toString();
        String vnp_Version = "2.1.0";
        String vnp_Command = "refund";
        String vnp_TmnCode = properties.getTmnCode();
        String vnp_TransactionType = "02"; // 02: Full Refund
        String vnp_TxnRef = payment.getTransactionRef();
        long amount = payment.getOrder().getTotalAmount().movePointRight(2).longValueExact();
        String vnp_Amount = String.valueOf(amount);
        String vnp_OrderInfo = "Hoan tien GD " + vnp_TxnRef + " ly do: " + cancelReason;
        String vnp_TransactionNo = payment.getGatewayTransactionNo() != null ? payment.getGatewayTransactionNo() : "";
        String vnp_TransactionDate = payment.getPayDate() != null ? payment.getPayDate() : "";
        String vnp_CreateBy = createBy != null ? createBy : "admin";
        String vnp_CreateDate = VNPAY_DATE_FORMAT.format(ZonedDateTime.now(HCMC));
        String vnp_IpAddr = ipAddr != null ? ipAddr : "127.0.0.1";

        String hashData = vnp_RequestId + "|" + vnp_Version + "|" + vnp_Command + "|" + vnp_TmnCode + "|" +
                vnp_TransactionType + "|" + vnp_TxnRef + "|" + vnp_Amount + "|" + vnp_TransactionNo + "|" +
                vnp_TransactionDate + "|" + vnp_CreateBy + "|" + vnp_CreateDate + "|" + vnp_IpAddr + "|" +
                vnp_OrderInfo;
                
        String vnp_SecureHash = hmacSHA512(properties.getHashSecret(), hashData);

        Map<String, String> requestData = new HashMap<>();
        requestData.put("vnp_RequestId", vnp_RequestId);
        requestData.put("vnp_Version", vnp_Version);
        requestData.put("vnp_Command", vnp_Command);
        requestData.put("vnp_TmnCode", vnp_TmnCode);
        requestData.put("vnp_TransactionType", vnp_TransactionType);
        requestData.put("vnp_TxnRef", vnp_TxnRef);
        requestData.put("vnp_Amount", vnp_Amount);
        requestData.put("vnp_TransactionNo", vnp_TransactionNo);
        requestData.put("vnp_TransactionDate", vnp_TransactionDate);
        requestData.put("vnp_CreateBy", vnp_CreateBy);
        requestData.put("vnp_CreateDate", vnp_CreateDate);
        requestData.put("vnp_IpAddr", vnp_IpAddr);
        requestData.put("vnp_OrderInfo", vnp_OrderInfo);
        requestData.put("vnp_SecureHash", vnp_SecureHash);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestData, headers);

        try {
            Map<String, String> response = restTemplate.postForObject(properties.getRefundUrl(), entity, Map.class);
            if (response != null) {
                String responseCode = response.get("vnp_ResponseCode");
                if (!"00".equals(responseCode)) {
                    throw new RuntimeException("Lỗi từ VNPAY: " + response.get("vnp_Message") + " (Mã: " + responseCode + ")");
                }
            } else {
                throw new RuntimeException("Không nhận được phản hồi từ VNPAY");
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi hoàn tiền VNPAY: " + e.getMessage());
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getPaymentUrl())
                || !StringUtils.hasText(properties.getTmnCode())
                || !StringUtils.hasText(properties.getHashSecret())
                || !StringUtils.hasText(properties.getReturnUrl())) {
            throw new RuntimeException("Thiếu cấu hình VNPAY");
        }
    }

    public record CallbackResult(boolean ok,
                                 String rspCode,
                                 String message,
                                 Payment payment) {
        public static CallbackResult error(String rspCode, String message) {
            return new CallbackResult(false, rspCode, message, null);
        }

        public static CallbackResult success(String rspCode, String message, Payment payment) {
            return new CallbackResult(true, rspCode, message, payment);
        }
    }
}
