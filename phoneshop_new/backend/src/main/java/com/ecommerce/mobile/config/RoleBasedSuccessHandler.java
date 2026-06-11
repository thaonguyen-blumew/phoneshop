package com.ecommerce.mobile.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        boolean isManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ADMIN"));
        boolean isEmployee = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        if (isManager) {
            setDefaultTargetUrl("/admin/dashboard");
            setAlwaysUseDefaultTargetUrl(false); // Let SavedRequest take precedence if it's an admin URL
        } else if (isEmployee) {
            setDefaultTargetUrl("/admin/staff-dashboard");
            setAlwaysUseDefaultTargetUrl(false);
        } else {
            setDefaultTargetUrl("/");
            setAlwaysUseDefaultTargetUrl(false);
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
