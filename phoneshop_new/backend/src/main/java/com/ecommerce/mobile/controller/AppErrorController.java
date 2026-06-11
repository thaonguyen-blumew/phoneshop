package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
public class AppErrorController implements ErrorController {

    //@RequestMapping("/error")
    public ResponseEntity<ApiResponse<Object>> handleError(HttpServletRequest request) {
        Object statusCodeObj = request.getAttribute("jakarta.servlet.error.status_code");
        Object messageObj = request.getAttribute("jakarta.servlet.error.message");

        int statusCode = statusCodeObj != null ? Integer.parseInt(statusCodeObj.toString()) : 500;
        String message = messageObj != null ? messageObj.toString() : "An error occurred";

        return ResponseEntity.status(statusCode).body(ApiResponse.error(statusCode, message));
    }
}
