package com.ecommerce.mobile.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.mobile.entity.Cart;
import com.ecommerce.mobile.entity.CartItem;
import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.OrderItem;
import com.ecommerce.mobile.entity.ProductVariant;
import com.ecommerce.mobile.entity.Voucher;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.enums.PaymentMethod;
import com.ecommerce.mobile.enums.PaymentStatus;
import com.ecommerce.mobile.repository.OrderRepository;

@Service
public class OrderService {

    private final CartService cartService;
    private final CustomerService customerService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final ShipmentService shipmentService;
    private final VoucherService voucherService;
    private final VnpayService vnpayService;

    public OrderService(CartService cartService,
                        CustomerService customerService,
                        OrderRepository orderRepository,
                        PaymentService paymentService,
                        ShipmentService shipmentService,
                        VoucherService voucherService,
                        VnpayService vnpayService) {
        this.cartService = cartService;
        this.customerService = customerService;
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
        this.shipmentService = shipmentService;
        this.voucherService = voucherService;
        this.vnpayService = vnpayService;
    }

    @Transactional
    public Order placeOrder(String customerEmail,
                            String shippingName,
                            String shippingPhone,
                            String shippingAddress,
                            String shippingWard,
                            String shippingDistrict,
                            String shippingCity,
                            String voucherCode,
                            PaymentMethod paymentMethod,
                            Long cartId,
                            List<Long> itemIds) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        Cart cart = cartService.getCartByCustomerEmail(customerEmail);

        if (cart.getCartId() == null || !cart.getCartId().equals(cartId)) {
            throw new RuntimeException("Giỏ hàng không hợp lệ");
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống");
        }

        List<CartItem> itemsToProcess;
        if (itemIds != null && !itemIds.isEmpty()) {
            itemsToProcess = cart.getItems().stream()
                    .filter(i -> itemIds.contains(i.getCartItemId()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            itemsToProcess = new ArrayList<>(cart.getItems());
        }

        if (itemsToProcess.isEmpty()) {
            throw new RuntimeException("Không có sản phẩm nào được chọn để thanh toán");
        }

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod == null ? PaymentMethod.COD : paymentMethod);
        order.setShippingName(shippingName);
        order.setShippingPhone(shippingPhone);
        order.setShippingAddress(shippingAddress);
        order.setShippingWard(shippingWard);
        order.setShippingDistrict(shippingDistrict);
        order.setShippingCity(shippingCity);
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : itemsToProcess) {
            ProductVariant variant = cartItem.getVariant();
            if (variant == null) {
                continue;
            }
            if (variant.getProduct() != null && variant.getProduct().getStatus() == com.ecommerce.mobile.enums.ProductStatus.INACTIVE) {
                throw new RuntimeException("Sản phẩm '" + cartItem.getProductName() + "' đã ngừng kinh doanh và không thể mua.");
            }
            Integer quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
            if (quantity < 1) {
                continue;
            }

            if (variant.getStockQty() != null && quantity > variant.getStockQty()) {
                throw new RuntimeException("Vượt quá số lượng tồn kho (" + variant.getStockQty() + ")");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(variant);
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(quantity);
            order.getItems().add(orderItem);

            if (cartItem.getSubtotal() != null) {
                total = total.add(cartItem.getSubtotal());
            }

            Integer currentStock = variant.getStockQty() == null ? 0 : variant.getStockQty();
            variant.setStockQty(currentStock - quantity);
        }

        // Logic phí ship vùng miền thực tế
        BigDecimal shippingFee;
        if (total.compareTo(new BigDecimal("10000000")) >= 0) {
            shippingFee = BigDecimal.ZERO;
        } else {
            // Kiểm tra nội thành (Hà Nội, TP. Hồ Chí Minh)
            if (shippingCity != null && (shippingCity.contains("Hà Nội") || shippingCity.contains("Hồ Chí Minh"))) {
                shippingFee = new BigDecimal("30000");
            } else {
                shippingFee = new BigDecimal("50000");
            }
        }
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(BigDecimal.ZERO);

        VoucherService.VoucherApplyResult voucherApplyResult = voucherService.resolveVoucher(voucherCode, total);
        if (voucherApplyResult != null) {
            Voucher voucher = voucherApplyResult.voucher();
            order.setVoucher(voucher);
            order.setDiscountAmount(voucherApplyResult.discountAmount());
            voucherService.consumeVoucher(voucher);
        }

        order.setTotalAmount(total.add(order.getShippingFee()).subtract(order.getDiscountAmount()));
        order = orderRepository.save(order);
        order.getPayments().add(paymentService.createPayment(order, order.getPaymentMethod()));
        Order saved = orderRepository.save(order);
        
        if (itemIds != null && !itemIds.isEmpty()) {
            cartService.removeItems(customerEmail, itemIds);
        } else {
            cartService.clearCart(customerEmail);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerEmail(String customerEmail) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        return orderRepository.findByCustomerUserIDOrderByCreatedAtDesc(customer.getUserID());
    }

    @Transactional(readOnly = true)
    public Order getOrderDetailByCustomerEmail(String customerEmail, Long orderId) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        return orderRepository.findDetailedByOrderId(orderId)
                .filter(order -> order.getCustomer() != null
                        && order.getCustomer().getUserID().equals(customer.getUserID()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Order getOrderByIdForStaff(Long orderId) {
        return orderRepository.findDetailedByOrderId(orderId).orElse(null);
    }

    @Transactional
    public Order cancelOrderByCustomerEmail(String customerEmail, Long orderId, String reason) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        Order order = orderRepository.findDetailedByOrderId(orderId)
                .filter(o -> o.getCustomer() != null
                        && o.getCustomer().getUserID().equals(customer.getUserID()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!canCustomerCancel(order.getStatus())) {
            throw new RuntimeException("Đơn hàng không thể hủy ở trạng thái hiện tại");
        }

        return cancelOrderInternal(order, reason, "Khách hàng hủy đơn");
    }

    @Transactional
    public Order completeOrderByCustomerEmail(String customerEmail, Long orderId) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        Order order = orderRepository.findDetailedByOrderId(orderId)
                .filter(o -> o.getCustomer() != null
                        && o.getCustomer().getUserID().equals(customer.getUserID()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.SHIPPING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Đơn hàng chưa được giao, không thể hoàn tất.");
        }

        order.setStatus(OrderStatus.DELIVERED);
        appendStatusNote(order, OrderStatus.DELIVERED);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrdersForStaff() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Order> getFilteredOrdersForStaff(OrderStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return orderRepository.filterOrders(status, start, end);
    }

    @Transactional(readOnly = true)
    public List<Order> getFilteredOrdersForCustomer(String customerEmail, OrderStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        return orderRepository.filterCustomerOrders(customer.getUserID(), status, start, end);
    }

    @Transactional(readOnly = true)
    public Order getOrderForStaff(Long orderId) {
        return orderRepository.findDetailedByOrderId(orderId).orElse(null);
    }

    @Transactional
    public Order receiveOrderForStaff(Long orderId) {
        Order order = orderRepository.findDetailedByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng đã hủy, không thể tiếp nhận");
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Đơn hàng đã giao, không thể tiếp nhận");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể tiếp nhận đơn ở trạng thái chờ xác nhận");
        }

        if (order.getPaymentMethod() == PaymentMethod.VN_PAY) {
            if (order.getLatestPayment() == null || order.getLatestPayment().getStatus() != PaymentStatus.SUCCESS) {
                throw new RuntimeException("Đơn VN_PAY chưa thanh toán thành công, không thể tiếp nhận");
            }
        }

        if (order.getLatestPayment() != null
                && order.getPaymentMethod() == PaymentMethod.COD
                && order.getLatestPayment().getStatus() == PaymentStatus.PENDING) {
            order.getLatestPayment().setResponseMessage("Nhân viên đã tiếp nhận đơn COD, chờ thu tiền khi giao hàng.");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        appendStatusNote(order, OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        shipmentService.recordLocalOrderStatus(saved, OrderStatus.CONFIRMED);
        return saved;
    }

    @Transactional
    public Order advanceOrderStatusForStaff(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findDetailedByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (newStatus == null) {
            throw new RuntimeException("Trạng thái không hợp lệ");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng đã hủy, không thể thay đổi trạng thái");
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Đơn hàng đã giao, không thể thay đổi trạng thái");
        }
        if (newStatus == OrderStatus.CANCELLED) {
            return cancelOrderInternal(order, "Nhân viên hủy đơn", "Nhân viên hủy đơn");
        }

        if (!isNextStaffStatus(order.getStatus(), newStatus)) {
            throw new RuntimeException("Chỉ được chuyển sang bước kế tiếp của đơn hàng");
        }

        if (order.getStatus() == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            ensurePaymentReadyForReception(order);
        }

        order.setStatus(newStatus);
        appendStatusNote(order, newStatus);
        Order saved = orderRepository.save(order);
        shipmentService.recordLocalOrderStatus(saved, newStatus);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<OrderStatus> getAllowedNextStatusesForStaff(Long orderId) {
        Order order = orderRepository.findDetailedByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            return Collections.emptyList();
        }

        List<OrderStatus> statuses = new ArrayList<>();
        if (order.getStatus() == OrderStatus.PENDING) {
            statuses.add(OrderStatus.CONFIRMED);
        } else if (order.getStatus() == OrderStatus.CONFIRMED) {
            statuses.add(OrderStatus.PACKING);
        } else if (order.getStatus() == OrderStatus.PACKING) {
            statuses.add(OrderStatus.SHIPPING);
        } else if (order.getStatus() == OrderStatus.SHIPPING) {
            statuses.add(OrderStatus.DELIVERED);
        }
        return statuses;
    }

    @Transactional(readOnly = true)
    public boolean canStaffCancel(Order order) {
        return order != null
                && order.getStatus() != OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.DELIVERED;
    }

    @Transactional(readOnly = true)
    public String getStaffStatusGuide(Order order) {
        if (order == null) {
            return "—";
        }
        return switch (order.getStatus()) {
            case PENDING -> "Nhân viên kiểm tra đơn và thanh toán, rồi tiếp nhận.";
            case CONFIRMED -> "Đơn đã được tiếp nhận, chuẩn bị sang bước đóng gói.";
            case PACKING -> "Kho đang đóng gói, sau đó bàn giao giao hàng.";
            case SHIPPING -> "Đơn đang trên đường giao.";
            case DELIVERED -> "Đơn đã giao thành công.";
            case CANCELLED -> "Đơn đã bị hủy.";
        };
    }

    private Order cancelOrderInternal(Order order, String reason, String actorLabel) {
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item == null || item.getVariant() == null || item.getQuantity() == null) {
                    continue;
                }
                ProductVariant variant = item.getVariant();
                Integer currentStock = variant.getStockQty() == null ? 0 : variant.getStockQty();
                variant.setStockQty(currentStock + item.getQuantity());
            }
        }

        if (order.getPayments() != null) {
            order.getPayments().forEach(payment -> {
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    if (order.getPaymentMethod() == PaymentMethod.VN_PAY) {
                        vnpayService.refundPayment(payment, reason, actorLabel, "127.0.0.1");
                    }
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setResponseMessage("Đơn hàng đã bị hủy. Đã hoàn tiền.");
                } else if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setResponseMessage(actorLabel + (reason == null || reason.isBlank() ? "" : ": " + reason.trim()));
                }
            });
        }

        order.setStatus(OrderStatus.CANCELLED);
        appendStatusNote(order, OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        shipmentService.recordLocalOrderStatus(saved, OrderStatus.CANCELLED);
        return saved;
    }

    private boolean isNextStaffStatus(OrderStatus current, OrderStatus target) {
        return (current == OrderStatus.PENDING && target == OrderStatus.CONFIRMED)
                || (current == OrderStatus.CONFIRMED && target == OrderStatus.PACKING)
                || (current == OrderStatus.PACKING && target == OrderStatus.SHIPPING)
                || (current == OrderStatus.SHIPPING && target == OrderStatus.DELIVERED);
    }

    private void ensurePaymentReadyForReception(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.VN_PAY) {
            if (order.getLatestPayment() == null || order.getLatestPayment().getStatus() != PaymentStatus.SUCCESS) {
                throw new RuntimeException("Đơn VN_PAY chưa thanh toán thành công, không thể tiếp nhận");
            }
            return;
        }
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            if (order.getLatestPayment() == null) {
                throw new RuntimeException("Đơn COD chưa có bản ghi thanh toán");
            }
            if (order.getLatestPayment().getStatus() == PaymentStatus.FAILED) {
                throw new RuntimeException("Thanh toán COD đang ở trạng thái lỗi, không thể tiếp nhận");
            }
        }
    }

    private void appendStatusNote(Order order, OrderStatus newStatus) {
        LocalDateTime now = LocalDateTime.now();
        if (newStatus == OrderStatus.CONFIRMED) order.setConfirmedAt(now);
        else if (newStatus == OrderStatus.SHIPPING) order.setShippingAt(now);
        else if (newStatus == OrderStatus.DELIVERED) order.setDeliveredAt(now);
        else if (newStatus == OrderStatus.CANCELLED) order.setCancelledAt(now);

        if (order.getLatestPayment() == null) {
            return;
        }
        switch (newStatus) {
            case CONFIRMED -> order.getLatestPayment().setResponseMessage("Đơn hàng đã được tiếp nhận.");
            case PACKING -> order.getLatestPayment().setResponseMessage("Đơn hàng đang được đóng gói.");
            case SHIPPING -> order.getLatestPayment().setResponseMessage("Đơn hàng đang được giao.");
            case DELIVERED -> order.getLatestPayment().setResponseMessage("Đơn hàng đã giao thành công.");
            default -> { }
        }
    }

    private boolean canCustomerCancel(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    private String generateOrderCode() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
