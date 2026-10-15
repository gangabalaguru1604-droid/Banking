package com.loanproject.loan_app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

    @Value("${security.trusted-proxies.enabled:false}")
    private boolean trustedProxiesEnabled;

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        if (trustedProxiesEnabled) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // First IP in comma-separated list is the client IP
                String[] ips = xForwardedFor.split(",");
                if (ips.length > 0) {
                    return ips[0].trim();
                }
            }

            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "UNKNOWN";
    }
}
