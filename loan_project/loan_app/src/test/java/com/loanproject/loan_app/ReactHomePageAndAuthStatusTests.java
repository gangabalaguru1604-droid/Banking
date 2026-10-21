package com.loanproject.loan_app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class ReactHomePageAndAuthStatusTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / forwards to /index.html when React static bundle is present")
    public void testHomePageServesReactBundle() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @DisplayName("GET /jsp-home returns legacy index JSP view")
    public void testLegacyJspHomePageRoute() throws Exception {
        mockMvc.perform(get("/jsp-home"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET /api/auth/status returns unauthenticated when no session exists")
    public void testAuthStatusUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("GET /api/auth/status returns authenticated user details when authenticated")
    public void testAuthStatusAuthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/status")
                .with(user("testcustomer@bank.com").roles("CUSTOMER"))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("POST /session/keep-alive without active session returns 401")
    public void testKeepAliveNoSession() throws Exception {
        mockMvc.perform(post("/session/keep-alive").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
