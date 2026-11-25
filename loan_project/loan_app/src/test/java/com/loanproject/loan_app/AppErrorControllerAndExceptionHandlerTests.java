package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;

import com.loanproject.loan_app.controller.AppErrorController;
import com.loanproject.loan_app.controller.GlobalExceptionHandler;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletResponse;

public class AppErrorControllerAndExceptionHandlerTests {

    private AppErrorController errorController;
    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        errorController = new AppErrorController();
        exceptionHandler = new GlobalExceptionHandler();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AppErrorController renders error view for 404 with helpful suggestions")
    void testHandleErrorHtml404() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelMap model = new ExtendedModelMap();

        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/non-existent-page");

        String view = errorController.handleErrorHtml(request, response, model);

        assertEquals("error", view);
        assertEquals(404, response.getStatus());
        assertEquals(404, model.get("statusCode"));
        assertEquals("Page Not Found", model.get("errorTitle"));
        assertEquals("/non-existent-page", model.get("requestPath"));
        assertNotNull(model.get("errorSuggestion"));
    }

    @Test
    @DisplayName("AppErrorController renders error view for 403 Forbidden")
    void testHandleErrorHtml403() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelMap model = new ExtendedModelMap();

        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/admin/system");

        String view = errorController.handleErrorHtml(request, response, model);

        assertEquals("error", view);
        assertEquals(403, response.getStatus());
        assertEquals(403, model.get("statusCode"));
        assertEquals("Access Denied or Session Expired", model.get("errorTitle"));
        assertNotNull(model.get("errorMessage"));
    }

    @Test
    @DisplayName("AppErrorController returns structured JSON response for API calls")
    void testHandleErrorJson() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/loans/process");

        ResponseEntity<Map<String, Object>> entity = errorController.handleErrorJson(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals(true, entity.getBody().get("error"));
        assertEquals(500, entity.getBody().get("status"));
        assertEquals("Internal Server Error", entity.getBody().get("title"));
    }

    @Test
    @DisplayName("GlobalExceptionHandler redirects anonymous or unauthenticated access to login with expired param")
    void testAccessDeniedUnauthenticatedRedirectsToLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // No authentication in context (anonymous)
        Object result = exceptionHandler.handleAccessDenied(request, response, new AccessDeniedException("Access is denied"));

        assertEquals("redirect:/login?expired=true", result);
    }

    @Test
    @DisplayName("GlobalExceptionHandler forwards authenticated user access denied to /error with 403")
    void testAccessDeniedAuthenticatedForwardsToError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Set authenticated user in context
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "customer@bank.com", "N/A", AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Object result = exceptionHandler.handleAccessDenied(request, response, new AccessDeniedException("Access is denied"));

        assertEquals("forward:/error", result);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals(403, request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
    }

    @Test
    @DisplayName("GlobalExceptionHandler catches general exceptions and forwards to /error with 500")
    void testGeneralExceptionForwardsToError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object result = exceptionHandler.handleGeneralException(request, response, new RuntimeException("Database timeout"));

        assertEquals("forward:/error", result);
        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(500, request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
    }
}
