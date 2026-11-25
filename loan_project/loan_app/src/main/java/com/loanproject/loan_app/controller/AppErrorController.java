package com.loanproject.loan_app.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Replaces Spring Boot's default BasicErrorController to eliminate the Whitelabel Error Page.
 * Routes HTML requests to a branded error.jsp and API requests to structured JSON.
 */
@Controller
public class AppErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(AppErrorController.class);

    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public String handleErrorHtml(HttpServletRequest request, HttpServletResponse response, ModelMap model) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            statusCode = 500;
        }

        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (requestUri == null) {
            requestUri = request.getRequestURI();
        }

        String customMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (exception != null) {
            log.error("Error occurred [{}] on URI [{}]: ", statusCode, requestUri, exception);
        } else {
            log.warn("Error dispatched [{}] on URI [{}] - message: {}", statusCode, requestUri, customMessage);
        }

        ErrorViewDetails details = resolveErrorDetails(statusCode, customMessage);

        response.setStatus(statusCode);
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", details.title());
        model.addAttribute("errorMessage", details.message());
        model.addAttribute("errorSuggestion", details.suggestion());
        model.addAttribute("requestPath", requestUri != null ? requestUri : "");

        return "error";
    }

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleErrorJson(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            statusCode = 500;
        }

        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String customMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        ErrorViewDetails details = resolveErrorDetails(statusCode, customMessage);

        Map<String, Object> body = new HashMap<>();
        body.put("error", true);
        body.put("status", statusCode);
        body.put("title", details.title());
        body.put("message", details.message());
        body.put("path", requestUri != null ? requestUri : "");

        HttpStatus status;
        try {
            status = HttpStatus.valueOf(statusCode);
        } catch (Exception e) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(status).body(body);
    }

    private ErrorViewDetails resolveErrorDetails(int statusCode, String customMessage) {
        return switch (statusCode) {
            case 400 -> new ErrorViewDetails(
                "Invalid Request",
                customMessage != null && !customMessage.isBlank() ? customMessage : "The request contains invalid parameters or malformed data.",
                "Please verify the information entered and try again."
            );
            case 401 -> new ErrorViewDetails(
                "Authentication Required",
                "You must be logged in to view this protected resource.",
                "Please sign in with your credentials to continue."
            );
            case 403 -> new ErrorViewDetails(
                "Access Denied or Session Expired",
                customMessage != null && !customMessage.isBlank() ? customMessage : "Your session may have timed out after 10 minutes of inactivity, or your account lacks the required permissions.",
                "If your session expired, please log in again. If you believe this is an error, contact your bank administrator."
            );
            case 404 -> new ErrorViewDetails(
                "Page Not Found",
                customMessage != null && !customMessage.isBlank() ? customMessage : "The requested page or endpoint could not be found.",
                "Please check the URL for typos or use the navigation links below."
            );
            case 405 -> new ErrorViewDetails(
                "Action Not Allowed",
                "The requested HTTP method is not supported for this address (e.g. refreshing a submitted form).",
                "Please return to the dashboard or use the provided buttons to submit your action."
            );
            case 500 -> new ErrorViewDetails(
                "Internal Server Error",
                "A temporary issue occurred while processing your request.",
                "Our team has been notified. Please try again in a few moments."
            );
            case 503 -> new ErrorViewDetails(
                "Service Temporarily Unavailable",
                "The service is currently undergoing maintenance or is experiencing high load.",
                "Please refresh the page in a few moments."
            );
            default -> new ErrorViewDetails(
                "Unexpected Error",
                customMessage != null && !customMessage.isBlank() ? customMessage : "An unexpected error occurred while processing your request.",
                "Please return to the dashboard or log in again."
            );
        };
    }

    private record ErrorViewDetails(String title, String message, String suggestion) {}
}
