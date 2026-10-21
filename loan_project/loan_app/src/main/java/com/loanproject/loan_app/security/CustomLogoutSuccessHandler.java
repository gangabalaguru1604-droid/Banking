package com.loanproject.loan_app.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AuthAuditService auditService;
    private final String frontendUrl;

    public CustomLogoutSuccessHandler(
            AuthAuditService auditService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.auditService = auditService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        String username = authentication != null ? authentication.getName() : "UNKNOWN";
        auditService.logLogout(username, request);

        String baseUrl = frontendUrl.replaceAll("/+$", "");
        response.sendRedirect(baseUrl + "/login?logout=true");
    }
}
