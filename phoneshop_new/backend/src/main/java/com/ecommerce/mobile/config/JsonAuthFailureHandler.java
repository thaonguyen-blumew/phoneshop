package com.ecommerce.mobile.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.ecommerce.mobile.entity.User;
import com.ecommerce.mobile.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonAuthFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public JsonAuthFailureHandler(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String message = resolveFailureMessage(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 401);
        body.put("message", message);
        body.put("data", null);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String resolveFailureMessage(HttpServletRequest request) {
        String email = request.getParameter("username");
        String password = request.getParameter("password");
        if (email == null || email.isBlank()) {
            return "Tài khoản không tồn tại";
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "Tài khoản không tồn tại";
        }
        if (user.getIsActive() == null || !user.getIsActive()) {
            return "Tài khoản đã bị khóa";
        }
        if (password == null || !passwordEncoder.matches(password, user.getHashPassword())) {
            return "Sai mật khẩu";
        }
        return "Sai email hoặc mật khẩu";
    }
}
