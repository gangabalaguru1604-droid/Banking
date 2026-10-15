package com.loanproject.loan_app.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
public class SessionController {

    @PostMapping("/session/keep-alive")
    public ResponseEntity<Map<String, String>> keepAlive(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // Accessing session refreshes its lastAccessedTime when user clicks "Stay Logged In"
            session.setAttribute("LAST_TOUCH", System.currentTimeMillis());
            return ResponseEntity.ok(Map.of("status", "active"));
        }
        return ResponseEntity.status(401).body(Map.of("status", "expired"));
    }
}
