package com.ecommerce.mobile.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.Shipment;
import com.ecommerce.mobile.entity.ShipmentTrackingEvent;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.enums.ShipmentStatus;
import com.ecommerce.mobile.repository.OrderRepository;
import com.ecommerce.mobile.repository.ShipmentRepository;
import com.ecommerce.mobile.repository.ShipmentTrackingEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ShipmentService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingEventRepository shipmentTrackingEventRepository;
    private final GhnClient ghnClient;
    private final ObjectMapper objectMapper;

    public ShipmentService(OrderRepository orderRepository,
                           ShipmentRepository shipmentRepository,
                           ShipmentTrackingEventRepository shipmentTrackingEventRepository,
                           GhnClient ghnClient,
                           ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.shipmentTrackingEventRepository = shipmentTrackingEventRepository;
        this.ghnClient = ghnClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Shipment ensureShipmentForOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new RuntimeException("Đơn hàng không hợp lệ");
        }

        Shipment shipment = shipmentRepository.findByOrderOrderId(order.getOrderId()).orElse(null);
        boolean created = false;
        if (shipment == null) {
            shipment = new Shipment();
            shipment.setOrder(order);
            shipment.setCarrier("GHN");
            shipment.setClientOrderCode(order.getOrderCode());
            shipment.setStatus(ShipmentStatus.PENDING);
            shipment.setStatusMessage("Đơn hàng đã được bàn giao cho đơn vị vận chuyển.");
            created = true;
        }

        if (!StringUtils.hasText(shipment.getClientOrderCode())) {
            shipment.setClientOrderCode(order.getOrderCode());
        }
        if (!StringUtils.hasText(shipment.getCarrier())) {
            shipment.setCarrier("GHN");
        }
        if (shipment.getStatus() == null) {
            shipment.setStatus(ShipmentStatus.PENDING);
        }
        if (created) {
            shipment.setTrackingUrl(buildTrackingUrl(shipment.getClientOrderCode()));
        }

        shipment.setLastSyncedAt(LocalDateTime.now());
        Shipment saved = shipmentRepository.save(shipment);
        if (created) {
            appendEvent(saved, "LOCAL_CREATED", "ready_to_pick", ShipmentStatus.PENDING,
                    null, "Tạo bản ghi vận chuyển nội bộ", null);
            saved = shipmentRepository.save(saved);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentForOrder(Long orderId) {
        return shipmentRepository.findByOrderOrderId(orderId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ShipmentTrackingEvent> getEventsForOrder(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderOrderId(orderId).orElse(null);
        if (shipment == null) {
            return List.of();
        }
        return shipmentTrackingEventRepository.findByShipmentShipmentIdOrderByOccurredAtAsc(shipment.getShipmentId());
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentForCustomer(Long orderId, String customerEmail) {
        Order order = orderRepository.findDetailedByOrderId(orderId)
                .filter(o -> o.getCustomer() != null && customerEmail.equals(o.getCustomer().getEmail()))
                .orElse(null);
        if (order == null) {
            return null;
        }
        return shipmentRepository.findByOrderOrderId(orderId).orElse(null);
    }

    @Transactional
    public Shipment refreshFromGhn(Long orderId) {
        Order order = orderRepository.findDetailedByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        Shipment shipment = ensureShipmentForOrder(order);
        return refreshFromGhn(shipment);
    }

    @Transactional
    public Shipment refreshFromGhn(Shipment shipment) {
        if (shipment == null || shipment.getOrder() == null) {
            throw new RuntimeException("Vận đơn không hợp lệ");
        }
        String clientOrderCode = shipment.getClientOrderCode();
        if (!StringUtils.hasText(clientOrderCode) && shipment.getOrder() != null) {
            clientOrderCode = shipment.getOrder().getOrderCode();
            shipment.setClientOrderCode(clientOrderCode);
        }

        JsonNode node = ghnClient.tryFetchOrderDetailByClientCode(clientOrderCode);
        if (node == null) {
            shipment.setLastSyncedAt(LocalDateTime.now());
            if (!StringUtils.hasText(shipment.getStatusMessage())) {
                shipment.setStatusMessage("GHN chưa sẵn sàng. Giữ trạng thái vận chuyển nội bộ.");
            }
            return shipmentRepository.save(shipment);
        }
        applyGhnSnapshot(shipment, node, "GHN_REFRESH", true);
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Shipment recordLocalOrderStatus(Order order, OrderStatus orderStatus) {
        if (order == null || order.getOrderId() == null) {
            throw new RuntimeException("Đơn hàng không hợp lệ");
        }
        Shipment shipment = ensureShipmentForOrder(order);
        ShipmentStatus status = shipment.getStatus();
        String message;
        String eventType = "LOCAL_STATUS_CHANGE";

        if (orderStatus == OrderStatus.CONFIRMED) {
            status = ShipmentStatus.PENDING;
            message = "Đơn hàng đã tiếp nhận, chờ bàn giao cho GHN.";
        } else if (orderStatus == OrderStatus.PACKING) {
            status = ShipmentStatus.PENDING;
            message = "Kho đang đóng gói hàng hóa.";
        } else if (orderStatus == OrderStatus.SHIPPING) {
            status = ShipmentStatus.PENDING;
            message = "Đơn hàng đã bàn giao cho GHN, chờ đồng bộ lộ trình.";
            if (shipment.getGhnOrderCode() == null) {
                JsonNode created = ghnClient.tryCreateOrder(order, shipment);
                if (created != null) {
                    String ghnOrderCode = readText(created, "order_code", "OrderCode", "ghn_order_code");
                    if (StringUtils.hasText(ghnOrderCode)) {
                        shipment.setGhnOrderCode(ghnOrderCode);
                        shipment.setTrackingUrl(buildTrackingUrl(shipment.getClientOrderCode()));
                        message = "GHN đã tạo vận đơn: " + ghnOrderCode;
                    }
                    appendEvent(shipment, "GHN_CREATED", ghnOrderCode, ShipmentStatus.PENDING, null,
                            "Đã tạo vận đơn trên GHN", created.toString());
                }
            }
        } else if (orderStatus == OrderStatus.DELIVERED) {
            status = ShipmentStatus.DELIVERED;
            message = "Đơn hàng đã giao thành công.";
        } else if (orderStatus == OrderStatus.CANCELLED) {
            status = ShipmentStatus.CANCELLED;
            message = "Đơn hàng đã bị hủy.";
        } else {
            return shipment;
        }

        shipment.setStatus(status);
        shipment.setStatusMessage(message);
        shipment.setLastSyncedAt(LocalDateTime.now());
        shipment.setTrackingUrl(buildTrackingUrl(shipment.getClientOrderCode()));
        appendEvent(shipment, eventType, shipment.getGhnStatus(), status, null, message, null);
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Shipment processGhnWebhook(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new RuntimeException("Payload GHN trống");
        }

        JsonNode node = objectMapper.valueToTree(payload);
        JsonNode dataNode = node.has("data") && node.get("data").isObject() ? node.get("data") : node;
        String clientOrderCode = readText(dataNode, "ClientOrderCode", "client_order_code", "clientOrderCode", "OrderCode", "order_code", "orderCode");
        if (!StringUtils.hasText(clientOrderCode)) {
            throw new RuntimeException("GHN callback thiếu mã đơn");
        }

        Shipment shipment = shipmentRepository.findByClientOrderCode(clientOrderCode).orElse(null);
        if (shipment == null) {
            Order order = orderRepository.findByOrderCode(clientOrderCode).orElse(null);
            if (order == null) {
                throw new RuntimeException("Không tìm thấy shipment/order cho mã GHN: " + clientOrderCode);
            }
            shipment = ensureShipmentForOrder(order);
        }

        applyGhnSnapshot(shipment, dataNode, "GHN_WEBHOOK", true);
        return shipmentRepository.save(shipment);
    }

    private void applyGhnSnapshot(Shipment shipment, JsonNode node, String eventType, boolean recordEvent) {
        String rawStatus = readText(node, "Status", "status");
        String description = readText(node, "Description", "description");
        String reason = readText(node, "Reason", "reason");
        String warehouse = readText(node, "Warehouse", "warehouse");
        String ghnOrderCode = readText(node, "OrderCode", "order_code", "ghn_order_code", "ghnOrderCode");
        String clientOrderCode = readText(node, "ClientOrderCode", "client_order_code", "clientOrderCode");
        String timeText = readText(node, "Time", "time", "UpdatedDate", "updated_date", "updatedAt");

        ShipmentStatus mappedStatus = mapShipmentStatus(rawStatus);
        String message = buildStatusMessage(rawStatus, description, reason);

        boolean changed = !Objects.equals(shipment.getGhnStatus(), rawStatus)
                || !Objects.equals(shipment.getStatus(), mappedStatus)
                || !Objects.equals(shipment.getStatusMessage(), message)
                || !Objects.equals(shipment.getGhnOrderCode(), ghnOrderCode);

        shipment.setGhnStatus(rawStatus);
        shipment.setStatus(mappedStatus);
        shipment.setStatusMessage(message);
        shipment.setGhnOrderCode(StringUtils.hasText(ghnOrderCode) ? ghnOrderCode : shipment.getGhnOrderCode());
        shipment.setClientOrderCode(StringUtils.hasText(clientOrderCode) ? clientOrderCode : shipment.getClientOrderCode());
        shipment.setTrackingUrl(buildTrackingUrl(shipment.getClientOrderCode()));
        shipment.setLastSyncedAt(LocalDateTime.now());
        shipment.setRawPayload(node.toString());

        if (StringUtils.hasText(timeText)) {
            LocalDateTime parsed = tryParseDateTime(timeText);
            if (parsed != null) {
                if (mappedStatus == ShipmentStatus.DELIVERED) {
                    shipment.setExpectedDeliveryAt(parsed);
                }
            }
        }

        if (mappedStatus == ShipmentStatus.DELIVERED && shipment.getOrder() != null && shipment.getOrder().getStatus() != OrderStatus.CANCELLED) {
            shipment.getOrder().setStatus(OrderStatus.DELIVERED);
        } else if (mappedStatus == ShipmentStatus.CANCELLED || mappedStatus == ShipmentStatus.RETURNED) {
            if (shipment.getOrder() != null && shipment.getOrder().getStatus() != OrderStatus.DELIVERED) {
                shipment.getOrder().setStatus(OrderStatus.CANCELLED);
            }
        }

        if (recordEvent && (changed || shipment.getEvents() == null || shipment.getEvents().isEmpty())) {
            appendEvent(shipment, eventType, rawStatus, mappedStatus, warehouse,
                    message, node.toString());
        }
    }

    private void appendEvent(Shipment shipment,
                             String eventType,
                             String ghnStatus,
                             ShipmentStatus status,
                             String warehouse,
                             String description,
                             String rawPayload) {
        if (shipment.getEvents() == null) {
            shipment.setEvents(new java.util.ArrayList<>());
        }
        ShipmentTrackingEvent event = new ShipmentTrackingEvent();
        event.setShipment(shipment);
        event.setEventType(eventType);
        event.setGhnStatus(ghnStatus);
        event.setShipmentStatus(status);
        event.setWarehouse(warehouse);
        event.setDescription(description);
        event.setRawPayload(rawPayload);
        shipment.getEvents().add(event);
    }

    private ShipmentStatus mapShipmentStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return ShipmentStatus.PENDING;
        }
        String normalized = rawStatus.trim().toLowerCase();
        return switch (normalized) {
            case "ready_to_pick", "picking", "money_collect_picking" -> ShipmentStatus.PENDING;
            case "picked" -> ShipmentStatus.PICKED_UP;
            case "storing", "transporting", "sorting", "delivering", "money_collect_delivering" -> ShipmentStatus.IN_TRANSIT;
            case "delivered" -> ShipmentStatus.DELIVERED;
            case "cancel" -> ShipmentStatus.CANCELLED;
            case "return", "returning", "return_transporting", "return_sorting", "waiting_to_return" -> ShipmentStatus.RETURNING;
            case "returned" -> ShipmentStatus.RETURNED;
            case "delivery_fail", "return_fail", "exception", "damage", "lost" -> ShipmentStatus.EXCEPTION;
            default -> ShipmentStatus.IN_TRANSIT;
        };
    }

    private String buildStatusMessage(String rawStatus, String description, String reason) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(description)) {
            sb.append(description.trim());
        } else if (StringUtils.hasText(rawStatus)) {
            sb.append("GHN trạng thái: ").append(rawStatus.trim());
        } else {
            sb.append("Đang đồng bộ trạng thái với GHN");
        }
        if (StringUtils.hasText(reason)) {
            sb.append(" | Lý do: ").append(reason.trim());
        }
        return sb.toString();
    }

    private String readText(JsonNode node, String... names) {
        if (node == null || names == null) {
            return null;
        }
        for (String name : names) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (StringUtils.hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private LocalDateTime tryParseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String buildTrackingUrl(String clientOrderCode) {
        if (!StringUtils.hasText(clientOrderCode)) {
            return null;
        }
        return "https://donhang.ghn.vn/?order_code=" + clientOrderCode;
    }
}
