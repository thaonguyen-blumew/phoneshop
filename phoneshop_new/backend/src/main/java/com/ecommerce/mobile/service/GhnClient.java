package com.ecommerce.mobile.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.Shipment;
import com.ecommerce.mobile.config.GhnProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GhnClient {

    private static final String DETAIL_BY_CLIENT_CODE_PATH = "/shipping-order/detail-by-client-code";
    private static final String CREATE_ORDER_PATH = "/shipping-order/create";

    private final GhnProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GhnClient(GhnProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public JsonNode fetchOrderDetailByClientCode(String clientOrderCode) {
        validateConfiguration();
        if (!StringUtils.hasText(clientOrderCode)) {
            throw new RuntimeException("Client order code không hợp lệ");
        }

        try {
            String endpoint = normalizeBaseUrl(properties.getApiBaseUrl()) + DETAIL_BY_CLIENT_CODE_PATH;
            String payload = objectMapper.createObjectNode()
                    .put("client_order_code", clientOrderCode)
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Token", properties.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("GHN trả về HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) {
                return data.get(0);
            }
            if (data.isObject()) {
                return data;
            }
            return root;
        } catch (IOException ex) {
            throw new RuntimeException("Không đọc được phản hồi GHN: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GHN request bị ngắt: " + ex.getMessage(), ex);
        }
    }

    public JsonNode tryFetchOrderDetailByClientCode(String clientOrderCode) {
        if (!isConfigured() || !StringUtils.hasText(clientOrderCode)) {
            return null;
        }
        try {
            return fetchOrderDetailByClientCode(clientOrderCode);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public boolean isConfigured() {
        return properties != null
                && StringUtils.hasText(properties.getApiBaseUrl())
                && StringUtils.hasText(properties.getToken())
                && !properties.getToken().startsWith("YOUR_")
                && StringUtils.hasText(properties.getShopId())
                && !properties.getShopId().startsWith("YOUR_");
    }

    public JsonNode createOrder(Order order, Shipment shipment) {
        validateConfiguration();
        if (order == null || order.getOrderId() == null) {
            throw new RuntimeException("Đơn hàng không hợp lệ");
        }

        try {
            String endpoint = normalizeBaseUrl(properties.getApiBaseUrl()) + CREATE_ORDER_PATH;
            var payload = objectMapper.createObjectNode();
            payload.put("payment_type_id", properties.getPaymentTypeId() == null ? 2 : properties.getPaymentTypeId());
            payload.put("note", "Don hang " + order.getOrderCode());
            payload.put("required_note", properties.getRequiredNote());
            payload.put("from_name", safe(properties.getFromName()));
            payload.put("from_phone", safe(properties.getFromPhone()));
            payload.put("from_address", safe(properties.getFromAddress()));
            payload.put("from_ward_name", safe(properties.getFromWardName()));
            payload.put("from_district_name", safe(properties.getFromDistrictName()));
            payload.put("from_province_name", safe(properties.getFromProvinceName()));
            payload.put("return_phone", safe(properties.getReturnPhone()));
            payload.put("return_address", safe(properties.getReturnAddress()));
            payload.put("return_district_name", safe(properties.getReturnDistrictName()));
            payload.put("return_ward_name", safe(properties.getReturnWardName()));
            payload.put("return_province_name", safe(properties.getReturnProvinceName()));
            payload.put("client_order_code", order.getOrderCode());
            payload.put("to_name", safe(order.getShippingName()));
            payload.put("to_phone", safe(order.getShippingPhone()));
            payload.put("to_address", safe(order.getShippingAddress()));
            payload.put("to_ward_name", safe(order.getShippingWard()));
            payload.put("to_district_name", safe(order.getShippingDistrict()));
            payload.put("to_province_name", safe(order.getShippingCity()));
            payload.put("cod_amount", 0);
            payload.put("content", "Don hang " + order.getOrderCode());
            payload.put("weight", properties.getWeight() == null ? 200 : properties.getWeight());
            payload.put("length", properties.getLength() == null ? 15 : properties.getLength());
            payload.put("width", properties.getWidth() == null ? 15 : properties.getWidth());
            payload.put("height", properties.getHeight() == null ? 15 : properties.getHeight());
            payload.put("insurance_value", properties.getInsuranceValue() == null ? 0 : properties.getInsuranceValue());
            payload.put("service_type_id", properties.getServiceTypeId() == null ? 2 : properties.getServiceTypeId());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Token", properties.getToken())
                    .header("ShopId", properties.getShopId() == null ? "" : properties.getShopId())
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("GHN trả về HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (data.isArray() && !data.isEmpty()) {
                return data.get(0);
            }
            if (data.isObject()) {
                return data;
            }
            return root;
        } catch (IOException ex) {
            throw new RuntimeException("Không đọc được phản hồi GHN: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("GHN request bị ngắt: " + ex.getMessage(), ex);
        }
    }

    public JsonNode tryCreateOrder(Order order, Shipment shipment) {
        if (!isConfigured()) {
            return null;
        }
        try {
            return createOrder(order, shipment);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void validateConfiguration() {
        if (properties == null) {
            throw new RuntimeException("GHN chưa được cấu hình");
        }
        if (!StringUtils.hasText(properties.getApiBaseUrl())) {
            throw new RuntimeException("Thiếu cấu hình GHN apiBaseUrl");
        }
        if (!StringUtils.hasText(properties.getToken()) || properties.getToken().startsWith("YOUR_")) {
            throw new RuntimeException("Thiếu token GHN hợp lệ");
        }
        if (!StringUtils.hasText(properties.getShopId()) || properties.getShopId().startsWith("YOUR_")) {
            throw new RuntimeException("Thiếu ShopId GHN hợp lệ");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
