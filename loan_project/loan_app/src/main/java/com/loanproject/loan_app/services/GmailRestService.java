package com.loanproject.loan_app.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanproject.loan_app.security.DataMaskingUtils;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class GmailRestService {

    private static final Logger log = LoggerFactory.getLogger(GmailRestService.class);
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Value("${gmail.api.enabled:false}")
    private boolean enabled;

    @Value("${gmail.api.client-id:}")
    private String clientId;

    @Value("${gmail.api.client-secret:}")
    private String clientSecret;

    @Value("${gmail.api.refresh-token:}")
    private String refreshToken;

    @Value("${gmail.api.sender-email:${reminder.mail.from:security@bankofindia.co.in}}")
    private String senderEmail;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Cache the access token in memory until close to expiry
    private volatile String cachedAccessToken = null;
    private volatile long tokenExpiryEpochMs = 0;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && refreshToken != null && !refreshToken.isBlank();
    }

    public boolean isEnabled() {
        return enabled || isConfigured();
    }

    public String getSenderEmail() {
        return (senderEmail != null && !senderEmail.isBlank()) ? senderEmail.trim() : "security@bankofindia.co.in";
    }

    /**
     * Sends an email via Google Gmail REST API over HTTPS
     *
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param htmlBody HTML body content
     * @return Message ID on success, or null on failure
     */
    public String sendEmail(String toEmail, String subject, String htmlBody) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Gmail API email send aborted: recipient email is empty");
            return null;
        }

        String maskedEmail = DataMaskingUtils.maskEmail(toEmail);

        try {
            String accessToken = getOrRefreshAccessToken();
            if (accessToken == null) {
                log.error("Failed to obtain OAuth2 access token for Gmail API");
                return null;
            }

            // 1. Build MIME Message
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage mimeMessage = new MimeMessage(session);

            mimeMessage.setFrom(new InternetAddress(getSenderEmail(), "Bank of India Loan Management"));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail.trim()));
            mimeMessage.setSubject(subject, "UTF-8");
            mimeMessage.setContent(htmlBody, "text/html; charset=UTF-8");

            // 2. Encode to Base64URL without padding (RFC 4648)
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);
            String encodedRaw = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());

            // 3. Prepare JSON payload: {"raw": "..."}
            String jsonPayload = (objectMapper != null)
                    ? objectMapper.writeValueAsString(Map.of("raw", encodedRaw))
                    : "{\"raw\":\"" + encodedRaw + "\"}";

            // 4. Send POST request via HTTPS
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GMAIL_SEND_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                String messageId = extractField(response.body(), "id");
                String resolvedId = (messageId != null) ? messageId : "GMAIL-" + UUID.randomUUID();
                log.info("Email successfully sent via Gmail API over HTTPS to {} (ID: {})", maskedEmail, resolvedId);
                return resolvedId;
            } else {
                log.error("Gmail API error ({}) for recipient {}: {}", status, maskedEmail, response.body());
                return null;
            }

        } catch (Exception ex) {
            log.error("Exception sending email via Gmail API over HTTPS to {}: {}", maskedEmail, ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Refreshes the Google OAuth2 access token using the refresh_token over HTTPS
     */
    private synchronized String getOrRefreshAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < (tokenExpiryEpochMs - 60_000)) {
            return cachedAccessToken;
        }

        try {
            String formBody = "client_id=" + java.net.URLEncoder.encode(clientId.trim(), java.nio.charset.StandardCharsets.UTF_8)
                    + "&client_secret=" + java.net.URLEncoder.encode(clientSecret.trim(), java.nio.charset.StandardCharsets.UTF_8)
                    + "&refresh_token=" + java.net.URLEncoder.encode(refreshToken.trim(), java.nio.charset.StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token";

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKEN_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String body = response.body();
                String accessToken = extractField(body, "access_token");
                String expiresInStr = extractField(body, "expires_in");
                long expiresInSec = (expiresInStr != null) ? Long.parseLong(expiresInStr) : 3600;

                this.cachedAccessToken = accessToken;
                this.tokenExpiryEpochMs = System.currentTimeMillis() + (expiresInSec * 1000);
                log.info("Successfully refreshed Gmail API OAuth2 access token (valid for {}s)", expiresInSec);
                return accessToken;
            } else {
                log.error("Failed to refresh Gmail OAuth2 token. HTTP {}: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception ex) {
            log.error("Error refreshing Gmail OAuth2 token: {}", ex.getMessage());
            return null;
        }
    }

    private String extractField(String json, String field) {
        if (json == null || json.isBlank() || objectMapper == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode target = root.get(field);
            return (target != null && !target.isNull()) ? target.asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
