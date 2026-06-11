package com.ecommerce.mobile.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.service.ShipmentService;
import com.ecommerce.mobile.response.ApiResponse;

@RestController
@CrossOrigin("*")
public class GhnWebhookController {

    private final ShipmentService shipmentService;

    public GhnWebhookController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping("/webhook/ghn")
    public Map<String, Object> handleGhnWebhook(
            @RequestHeader(value = "Token", required = false) String token,
            @RequestBody Map<String, Object> payload) {
        
        // Return raw map as expected by webhook provider but log/process it
        shipmentService.processGhnWebhook(payload);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 200);
        response.put("message", "Success");
        return response;
    }
}
