package com.loanproject.loan_app.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Global exception interceptor to prevent raw uncaught exceptions from reaching
 * the servlet container or displaying Whitelabel error pages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken);

        // If user session expired or user is not logged in, redirect smoothly to login with session alert
        if (isAnonymous || "User not authenticated".equalsIgnoreCase(ex.getMessage())) {
            return "redirect:/login?expired=true";
        }

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", true, "status", 403, "message", "Access Denied: " + ex.getMessage()));
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Access Denied: You do not have permission to access this page.");
        return "forward:/error";
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotSupported(HttpServletRequest request, HttpServletResponse response, HttpRequestMethodNotSupportedException ex) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(Map.of("error", true, "status", 405, "message", ex.getMessage()));
        }

        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "This action cannot be performed directly. Please navigate from the dashboard or form.");
        return "forward:/error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFound(HttpServletRequest request, HttpServletResponse response, NoResourceFoundException ex) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", true, "status", 404, "message", "Resource not found: " + ex.getResourcePath()));
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "The requested resource could not be found.");
        return "forward:/error";
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleMissingParam(HttpServletRequest request, HttpServletResponse response, MissingServletRequestParameterException ex) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", true, "status", 400, "message", ex.getMessage()));
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Required parameter '" + ex.getParameterName() + "' is missing.");
        return "forward:/error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        log.error("Unhandled exception processing request [{}]: ", request.getRequestURI(), ex);

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "status", 500, "message", "An unexpected server error occurred."));
        }

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "An unexpected server error occurred while processing your request. Please try again later.");
        return "forward:/error";
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json") && !accept.contains("text/html");
    }
}
