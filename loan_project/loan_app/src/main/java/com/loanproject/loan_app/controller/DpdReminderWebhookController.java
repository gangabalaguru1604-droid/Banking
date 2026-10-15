package com.loanproject.loan_app.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loanproject.loan_app.services.DpdReminderService;

@RestController
@RequestMapping("/api/webhooks")
public class DpdReminderWebhookController {

    private static final Logger log = LoggerFactory.getLogger(DpdReminderWebhookController.class);

    private final DpdReminderService reminderService;

    @Autowired(required = false)
    private com.loanproject.loan_app.services.DpdReminderScheduler reminderScheduler;

    @Autowired(required = false)
    private com.loanproject.loan_app.repository.LoanReminderLogRepository reminderLogRepository;

    @Value("${reminder.webhook.secret:test_webhook_secret_patil_capitals_2026}")
    private String webhookSecret;

    @Autowired
    public DpdReminderWebhookController(DpdReminderService reminderService) {
        this.reminderService = reminderService;
    }

    public static class WebhookPayload {
        private String providerMessageId;
        private String eventType; // DELIVERED, BOUNCED, OPENED
        private String failureReason;
        private String timestamp;

        public String getProviderMessageId() {
            return providerMessageId;
        }

        public void setProviderMessageId(String providerMessageId) {
            this.providerMessageId = providerMessageId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    @PostMapping("/reminders")
    public ResponseEntity<?> handleProviderWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody WebhookPayload payload) {

        if (payload == null || payload.getProviderMessageId() == null || payload.getEventType() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook payload"));
        }

        // Validate webhook signature
        if (!isValidSignature(payload.getProviderMessageId(), payload.getEventType(), signature)) {
            log.warn("Invalid webhook signature received for providerMessageId: {}", payload.getProviderMessageId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid signature"));
        }

        boolean processed = reminderService.processProviderWebhook(
                payload.getProviderMessageId(),
                payload.getEventType(),
                payload.getFailureReason());

        if (processed) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "event", payload.getEventType()));
        } else {
            return ResponseEntity.ok(Map.of("status", "IGNORED", "message", "Event ignored or already finalized"));
        }
    }

    /**
     * Validates HMAC-SHA256 signature using the configured secret key.
     */
    public boolean isValidSignature(String providerMessageId, String eventType, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        try {
            String data = providerMessageId + ":" + eventType.toUpperCase();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            String expectedSignature = sb.toString();
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            log.error("Error verifying webhook signature: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Helper utility for external callers / tests to compute signatures.
     */
    public String generateSignature(String providerMessageId, String eventType) {
        try {
            String data = providerMessageId + ":" + eventType.toUpperCase();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Could not generate HMAC", ex);
        }
    }

    @PostMapping("/reminders/trigger")
    public ResponseEntity<?> triggerRemindersManually(
            @org.springframework.web.bind.annotation.RequestParam(value = "date", required = false) String dateStr) {
        java.time.LocalDate businessDate = (dateStr != null && !dateStr.isBlank())
                ? java.time.LocalDate.parse(dateStr.trim())
                : java.time.LocalDate.now();
        int processed = (reminderScheduler != null) ? reminderScheduler.runDailyReminderJob(businessDate) : 0;
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "businessDate", businessDate.toString(),
                "processedInstallments", processed,
                "message", "DPD Reminder Workflow executed successfully"
        ));
    }

    @org.springframework.web.bind.annotation.GetMapping("/reminders/logs")
    public ResponseEntity<?> listRecentReminderLogs() {
        if (reminderLogRepository == null) {
            return ResponseEntity.ok(java.util.List.of());
        }
        return ResponseEntity.ok(reminderLogRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
    }
}
