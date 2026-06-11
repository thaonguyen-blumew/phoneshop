package com.ecommerce.mobile.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.mobile.response.ApiResponse;

@org.springframework.web.bind.annotation.ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(RuntimeException ex, jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, ex.getMessage()));
        }
        
        // For web requests, let Spring's default error page handle it if it's a template error, 
        // to avoid any preset Content-Type conflicts.
        throw ex; // Re-throw to let Spring Boot handle it gracefully with its default error view.
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, ex.getMessage()));
        }
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<String> handleGenericException(Exception ex) {
        ex.printStackTrace();
        java.io.StringWriter sw = new java.io.StringWriter();
        ex.printStackTrace(new java.io.PrintWriter(sw));
        return org.springframework.http.ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(org.springframework.http.MediaType.TEXT_HTML)
                .body("<html><body><h1>HTTP ERROR 500</h1><pre>" + sw.toString() + "</pre>" + "<!-- padding to prevent browser overriding " + ".".repeat(1000) + " --></body></html>");
    }
}
