package com.cinema.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                response.sendRedirect("/admin.html?login=ok");
                return;
            }
            if ("ROLE_CASHIER".equals(role)) {
                response.sendRedirect("/cashier.html?login=ok");
                return;
            }
            if ("ROLE_CLIENT".equals(role)) {
                response.sendRedirect("/client.html?login=ok");
                return;
            }
        }
        response.sendRedirect("/");
    }
}
