package com.loanproject.loan_app.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanproject.loan_app.dto.ReminderMessageRequest;
import com.loanproject.loan_app.dto.ReminderSendResult;
import com.loanproject.loan_app.security.DataMaskingUtils;

import jakarta.mail.internet.MimeMessage;

@Service
public class DefaultEmailReminderProvider implements EmailReminderProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmailReminderProvider.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private GmailRestService gmailRestService;

    @Autowired(required = false)
    private ObjectMapper objectMapper;

    @Value("${reminder.email.provider:AUTO}")
    private String configuredProvider;

    @Value("${reminder.email.resend-api-key:}")
    private String resendApiKey;

    @Value("${reminder.email.sendgrid-api-key:}")
    private String sendgridApiKey;

    @Value("${reminder.email.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${reminder.email.api-url:}")
    private String emailApiUrl;

    @Value("${reminder.email.api-key:}")
    private String emailApiKey;

    @Value("${reminder.mail.from:reminders@indianbank.co.in}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${reminder.mail.simulate:false}")
    private boolean simulateSend;

    public String getEffectiveFromEmail() {
        if (fromEmail != null && !fromEmail.trim().isBlank()) {
            return fromEmail.trim();
        }
        if (mailUsername != null && !mailUsername.trim().isBlank()) {
            return mailUsername.trim();
        }
        return "reminders@indianbank.co.in";
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public ReminderSendResult sendReminderEmail(ReminderMessageRequest request) {
        if (request == null) {
            return ReminderSendResult.failure("Request must not be null", false);
        }
        if (request.getRecipientEmail() == null || request.getRecipientEmail().isBlank()) {
            return ReminderSendResult.failure("Recipient email is empty", false);
        }

        String maskedEmail = DataMaskingUtils.maskEmail(request.getRecipientEmail());
        String subject = buildSubject(request);
        String body = buildHtmlBody(request);

        String providerMode = resolveProviderMode();

        // 1. Simulation mode
        if (simulateSend || "SIMULATED".equalsIgnoreCase(providerMode)) {
            String mockMessageId = "EML-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("Simulated email dispatch to recipient {} (Message ID: {}): {}",
                    maskedEmail, mockMessageId, subject);
            return ReminderSendResult.success(mockMessageId);
        }

        // 2. Select the explicitly configured provider.
        if ("SMTP".equalsIgnoreCase(providerMode)) {
            if (mailSender == null) {
                log.error("SMTP is selected, but JavaMailSender is not configured.");
                return ReminderSendResult.failure(
                        "SMTP is selected, but JavaMailSender is unavailable. Check spring-boot-starter-mail and MAIL_* settings.",
                        false);
            }
            return sendViaSmtp(request, subject, body, maskedEmail);
        } else if ("RESEND".equalsIgnoreCase(providerMode)) {
            if (resendApiKey == null || resendApiKey.isBlank()) {
                return ReminderSendResult.failure("RESEND is selected, but RESEND_API_KEY is missing.", false);
            }
            return sendViaResend(request, subject, body, maskedEmail);
        } else if ("SENDGRID".equalsIgnoreCase(providerMode)) {
            if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
                return ReminderSendResult.failure("SENDGRID is selected, but SENDGRID_API_KEY is missing.", false);
            }
            return sendViaSendgrid(request, subject, body, maskedEmail);
        } else if ("BREVO".equalsIgnoreCase(providerMode)) {
            if (brevoApiKey == null || brevoApiKey.isBlank()) {
                return ReminderSendResult.failure("BREVO is selected, but BREVO_API_KEY is missing.", false);
            }
            return sendViaBrevo(request, subject, body, maskedEmail);
        } else if ("GMAIL_API".equalsIgnoreCase(providerMode) || "GMAIL".equalsIgnoreCase(providerMode)) {
            return sendViaGmailApi(request, subject, body, maskedEmail);
        } else if ("HTTP_REST".equalsIgnoreCase(providerMode)) {
            if (emailApiUrl == null || emailApiUrl.isBlank()) {
                return ReminderSendResult.failure("HTTP_REST is selected, but EMAIL_API_URL is missing.", false);
            }
            return sendViaGenericRest(request, subject, body, maskedEmail);
        }

        // AUTO mode: use whichever provider is configured. Do not report a fake success.
        if (gmailRestService != null && gmailRestService.isConfigured()) {
            return sendViaGmailApi(request, subject, body, maskedEmail);
        }
        if (mailSender != null && mailUsername != null && !mailUsername.isBlank()) {
            return sendViaSmtp(request, subject, body, maskedEmail);
        }

        return ReminderSendResult.failure(
                "No usable email provider is configured. Set EMAIL_PROVIDER_TYPE and the required credentials.",
                false);
    }

    private String resolveProviderMode() {
        String mode = (configuredProvider != null) ? configuredProvider.trim().toUpperCase() : "AUTO";
        if (!"AUTO".equals(mode)) {
            return mode;
        }
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            return "RESEND";
        }
        if (gmailRestService != null && gmailRestService.isConfigured()) {
            return "GMAIL_API";
        }
        if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
            return "SENDGRID";
        }
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            return "BREVO";
        }
        if (emailApiUrl != null && !emailApiUrl.isBlank()) {
            return "HTTP_REST";
        }
        if (mailSender != null && mailUsername != null && !mailUsername.isBlank()) {
            return "SMTP";
        }
        return "SIMULATED";
    }

    private ReminderSendResult sendViaGmailApi(ReminderMessageRequest request, String subject, String body, String maskedEmail) {
        log.info("Dispatching reminder email via HTTPS Gmail REST API to recipient {}", maskedEmail);
        if (gmailRestService == null || !gmailRestService.isConfigured()) {
            return ReminderSendResult.failure("Gmail API is selected, but GMAIL_* credentials are not fully configured.", false);
        }
        String messageId = gmailRestService.sendEmail(request.getRecipientEmail(), subject, body);
        if (messageId != null) {
            log.info("Email successfully accepted by Gmail API for recipient {} with ID {}", maskedEmail, messageId);
            return ReminderSendResult.success(messageId);
        } else {
            return ReminderSendResult.failure("Failed to send email via Gmail REST API", true);
        }
    }

    private ReminderSendResult sendViaResend(ReminderMessageRequest request, String subject, String body, String maskedEmail) {
        log.info("Dispatching reminder email via HTTPS Resend API to recipient {}", maskedEmail);
        try {
            Map<String, Object> payload = Map.of(
                    "from", getEffectiveFromEmail(),
                    "to", List.of(request.getRecipientEmail()),
                    "subject", subject,
                    "html", body
            );
            String json = serializePayload(payload);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code >= 200 && code < 300) {
                String id = extractJsonField(response.body(), "id");
                String providerId = (id != null) ? id : "RESEND-" + UUID.randomUUID();
                log.info("Email successfully accepted by Resend for recipient {} with ID {}", maskedEmail, providerId);
                return ReminderSendResult.success(providerId);
            } else if (code >= 400 && code < 500) {
                log.error("Resend API client error ({}) for recipient {}: {}", code, maskedEmail, response.body());
                return ReminderSendResult.failure("Resend client error " + code + ": " + response.body(), false);
            } else {
                log.warn("Resend API server error ({}) for recipient {}: {}", code, maskedEmail, response.body());
                return ReminderSendResult.failure("Resend server error " + code, true);
            }
        } catch (IOException | InterruptedException ex) {
            log.warn("Transient network error dispatching to Resend API for recipient {}: {}", maskedEmail, ex.getMessage());
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ReminderSendResult.failure("Resend network failure: " + ex.getMessage(), true);
        } catch (Exception ex) {
            log.error("Unexpected error calling Resend API for recipient {}: {}", maskedEmail, ex.getMessage());
            return ReminderSendResult.failure("Resend unexpected failure: " + ex.getMessage(), false);
        }
    }

    private ReminderSendResult sendViaSendgrid(ReminderMessageRequest request, String subject, String body, String maskedEmail) {
        log.info("Dispatching reminder email via HTTPS SendGrid API to recipient {}", maskedEmail);
        try {
            Map<String, Object> payload = Map.of(
                    "personalizations", List.of(Map.of("to", List.of(Map.of("email", request.getRecipientEmail())))),
                    "from", Map.of("email", getEffectiveFromEmail()),
                    "subject", subject,
                    "content", List.of(Map.of("type", "text/html", "value", body))
            );
            String json = serializePayload(payload);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + sendgridApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code >= 200 && code < 300) {
                String providerId = response.headers().firstValue("X-Message-Id").orElse("SG-" + UUID.randomUUID());
                log.info("Email successfully accepted by SendGrid for recipient {} with ID {}", maskedEmail, providerId);
                return ReminderSendResult.success(providerId);
            } else if (code >= 400 && code < 500) {
                log.error("SendGrid API client error ({}) for recipient {}: {}", code, maskedEmail, response.body());
                return ReminderSendResult.failure("SendGrid client error " + code + ": " + response.body(), false);
            } else {
                log.warn("SendGrid API server error ({}) for recipient {}: {}", code, maskedEmail, response.body());
                return ReminderSendResult.failure("SendGrid server error " + code, true);
            }
        } catch (IOException | InterruptedException ex) {
            log.warn("Transient network error dispatching to SendGrid API for recipient {}: {}", maskedEmail, ex.getMessage());
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ReminderSendResult.failure("SendGrid network failure: " + ex.getMessage(), true);
        } catch (Exception ex) {
            log.error("Unexpected error calling SendGrid API for recipient {}: {}", maskedEmail, ex.getMessage());
            return ReminderSendResult.failure("SendGrid unexpected failure: " + ex.getMessage(), false);
        }
    }

    private ReminderSendResult sendViaBrevo(ReminderMessageRequest request, String subject, String body, String maskedEmail) {
        log.info("Dispatching reminder email via HTTPS Brevo API to recipient {}", maskedEmail);
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("email", getEffectiveFromEmail()),
                    "to", List.of(Map.of("email", request.getRecipientEmail(), "name", request.getCustomerName())),
                    "subject", subject,
                    "htmlContent", body
            );
            String json = serializePayload(payload);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(10))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code >= 200 && code < 300) {
                String id = extractJsonField(response.body(), "messageId");
                String providerId = (id != null) ? id : "BREVO-" + UUID.randomUUID();
                log.info("Email successfully accepted by Brevo for recipient {} with ID {}", maskedEmail, providerId);
                return ReminderSendResult.success(providerId);
            } else if (code >= 400 && code < 500) {
                log.error("Brevo API client error ({}) for recipient {}: {}", code, maskedEmail, response.body());
                return ReminderSendResult.failure("Brevo client error " + code + ": " + response.body(), false);
            } else {
                log.warn("Brevo API server error ({}) for recipient {}: {}", code, maskedEmail, response.body());
                return ReminderSendResult.failure("Brevo server error " + code, true);
            }
        } catch (IOException | InterruptedException ex) {
            log.warn("Transient network error dispatching to Brevo API for recipient {}: {}", maskedEmail, ex.getMessage());
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ReminderSendResult.failure("Brevo network failure: " + ex.getMessage(), true);
        } catch (Exception ex) {
            log.error("Unexpected error calling Brevo API for recipient {}: {}", maskedEmail, ex.getMessage());
            return ReminderSendResult.failure("Brevo unexpected failure: " + ex.getMessage(), false);
        }
    }

    private ReminderSendResult sendViaGenericRest(ReminderMessageRequest request, String subject, String body, String maskedEmail) {
        log.info("Dispatching reminder email via HTTPS REST API endpoint to recipient {}", maskedEmail);
        try {
            Map<String, Object> payload = Map.of(
                    "from", fromEmail,
                    "to", request.getRecipientEmail(),
                    "subject", subject,
                    "html", body,
                    "account", request.getMaskedAccountNumber(),
                    "dpd", request.getDpdDay()
            );
            String json = serializePayload(payload);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(emailApiUrl.trim()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json");

            if (emailApiKey != null && !emailApiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + emailApiKey.trim());
            }

            HttpRequest httpRequest = builder.POST(HttpRequest.BodyPublishers.ofString(json)).build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code >= 200 && code < 300) {
                String id = extractJsonField(response.body(), "id");
                if (id == null) {
                    id = extractJsonField(response.body(), "messageId");
                }
                String providerId = (id != null) ? id : "REST-" + UUID.randomUUID();
                log.info("Email successfully accepted by HTTP REST provider for recipient {} with ID {}", maskedEmail, providerId);
                return ReminderSendResult.success(providerId);
            } else if (code >= 400 && code < 500) {
                return ReminderSendResult.failure("HTTP REST email provider client error " + code, false);
            } else {
                return ReminderSendResult.failure("HTTP REST email provider server error " + code, true);
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ReminderSendResult.failure("HTTP REST network error: " + ex.getMessage(), true);
        } catch (Exception ex) {
            return ReminderSendResult.failure("HTTP REST unexpected failure: " + ex.getMessage(), false);
        }
    }

    private ReminderSendResult sendViaSmtp(ReminderMessageRequest request, String subject, String body, String maskedEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getEffectiveFromEmail());
            helper.setTo(request.getRecipientEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            String providerMessageId = "EML-" + UUID.randomUUID().toString();
            message.setHeader("X-Message-ID", providerMessageId);
            message.setHeader("X-Loan-Account", request.getMaskedAccountNumber());
            message.setHeader("X-DPD-Day", String.valueOf(request.getDpdDay()));

            mailSender.send(message);
            log.info("Email sent successfully via SMTP to recipient {} with Provider ID {}", maskedEmail, providerMessageId);
            return ReminderSendResult.success(providerMessageId);
        } catch (MailAuthenticationException ex) {
            log.error("Authentication failure sending reminder email to recipient {}: {}", maskedEmail, ex.getMessage());
            return ReminderSendResult.failure("Email authentication failed: " + ex.getMessage(), false);
        } catch (MailException ex) {
            String errorMsg = ex.getMessage();
            boolean isConnectionError = errorMsg != null && (errorMsg.contains("ConnectException") || errorMsg.contains("Connection timed out") || errorMsg.contains("Connection refused"));
            if (isConnectionError) {
                log.warn("SMTP connection failed to host for recipient {}. Outbound SMTP ports (25, 465, 587) are typically blocked on cloud hosting platforms such as Render. Configure HTTPS transactional email provider via RESEND_API_KEY, SENDGRID_API_KEY, or BREVO_API_KEY. Details: {}",
                        maskedEmail, errorMsg);
            } else {
                log.warn("Transient failure sending reminder email via SMTP to recipient {}: {}", maskedEmail, errorMsg);
            }
            return ReminderSendResult.failure("Email dispatch transient failure: " + errorMsg, true);
        } catch (Exception ex) {
            log.error("Unexpected error sending reminder email to recipient {}: {}", maskedEmail, ex.getMessage(), ex);
            return ReminderSendResult.failure("Email unexpected failure: " + ex.getMessage(), false);
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        if (objectMapper != null) {
            try {
                return objectMapper.writeValueAsString(payload);
            } catch (Exception ignored) {
            }
        }
        // Fallback simple JSON serializer
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : payload.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            if (e.getValue() instanceof String) {
                sb.append("\"").append(escapeJson((String) e.getValue())).append("\"");
            } else if (e.getValue() instanceof List) {
                sb.append("[");
                List<?> list = (List<?>) e.getValue();
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    Object item = list.get(i);
                    if (item instanceof String) {
                        sb.append("\"").append(escapeJson((String) item)).append("\"");
                    } else {
                        sb.append(serializePayload((Map<String, Object>) item));
                    }
                }
                sb.append("]");
            } else if (e.getValue() instanceof Map) {
                sb.append(serializePayload((Map<String, Object>) e.getValue()));
            } else {
                sb.append(e.getValue());
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractJsonField(String body, String fieldName) {
        if (body == null || fieldName == null) return null;
        try {
            if (objectMapper != null) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);
                if (root.has(fieldName)) {
                    return root.get(fieldName).asText();
                }
            }
        } catch (Exception ignored) {
        }
        // Regex fallback
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher m = p.matcher(body);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public String previewHtml(ReminderMessageRequest request) {
        return buildHtmlBody(request);
    }

    public String previewSubject(ReminderMessageRequest request) {
        return buildSubject(request);
    }

    private String buildSubject(ReminderMessageRequest request) {
        String account = request.getMaskedAccountNumber();
        switch (request.getDpdDay()) {
            case 1:
                return "Friendly Reminder: EMI Payment Due for Loan " + account;
            case 2:
                return "Second Notice: Overdue EMI Payment for Loan " + account;
            case 3:
                return "Final Notice: Overdue EMI Payment Prior to IVR Escalation for Loan " + account;
            default:
                return "Repayment Reminder for Loan " + account;
        }
    }

    private String buildHtmlBody(ReminderMessageRequest request) {
        String dueDateStr = request.getDueDate() != null ? request.getDueDate().format(DATE_FORMATTER) : "N/A";
        BigDecimal overdue = request.getOverdueAmount() != null ? request.getOverdueAmount() : BigDecimal.ZERO;
        BigDecimal penalty = request.getPenaltyAmount();
        BigDecimal total = overdue.add(penalty);
        String paymentUrl = request.getPaymentUrl() != null ? request.getPaymentUrl() : "#";
        String helpdesk = request.getHelpdeskNumber() != null ? request.getHelpdeskNumber() : "1800-200-8899";

        String headerTitle;
        String introMessage;
        String headerBg;

        if (request.getDpdDay() == 1) {
            headerTitle = "Friendly Repayment Reminder";
            headerBg = "#1e3a8a"; // Classic blue
            introMessage = "We hope you are doing well. This is a gentle reminder that your loan installment was due on <strong>"
                    + dueDateStr + "</strong> and remains unpaid. Please clear this amount to maintain an unblemished credit record.";
        } else if (request.getDpdDay() == 2) {
            headerTitle = "Second Notice - Overdue Repayment";
            headerBg = "#d97706"; // Amber / warning
            introMessage = "This is our second notice regarding your overdue loan installment which was due on <strong>"
                    + dueDateStr + "</strong>. Late payment penalties may apply. Please settle your dues immediately.";
        } else {
            headerTitle = "Final Notice Before Escalation";
            headerBg = "#b91c1c"; // Urgent red
            introMessage = "<strong>CRITICAL NOTICE:</strong> Your loan installment is now 3 days overdue. This is the final email reminder before your account is escalated to automated IVR outreach and field collections.";
        }

        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'><style>"
                + "body { font-family: 'Segoe UI', Tahoma, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; color: #1e293b; }"
                + ".card { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; }"
                + ".header { background-color: " + headerBg + "; color: #ffffff; padding: 24px; text-align: center; }"
                + ".header h2 { margin: 0; font-size: 20px; font-weight: 600; }"
                + ".content { padding: 24px; font-size: 14px; line-height: 1.6; }"
                + ".summary-table { width: 100%; border-collapse: collapse; margin: 20px 0; background: #f1f5f9; border-radius: 6px; overflow: hidden; }"
                + ".summary-table td { padding: 10px 16px; border-bottom: 1px solid #e2e8f0; }"
                + ".summary-table td.label { font-weight: 600; color: #475569; width: 45%; }"
                + ".summary-table td.value { font-weight: 700; color: #0f172a; text-align: right; }"
                + ".btn-container { text-align: center; margin: 24px 0; }"
                + ".btn { display: inline-block; background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 15px; }"
                + ".footer { padding: 16px 24px; background: #f8fafc; border-top: 1px solid #e2e8f0; font-size: 12px; color: #64748b; text-align: center; }"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='header'><h2>" + headerTitle + "</h2></div>"
                + "<div class='content'>"
                + "<p>Dear " + escapeHtml(request.getCustomerName()) + ",</p>"
                + "<p>" + introMessage + "</p>"
                + "<table class='summary-table'>"
                + "<tr><td class='label'>Loan Account:</td><td class='value'>" + escapeHtml(request.getMaskedAccountNumber()) + "</td></tr>"
                + "<tr><td class='label'>Due Date:</td><td class='value'>" + dueDateStr + "</td></tr>"
                + "<tr><td class='label'>Overdue Principal/Interest:</td><td class='value'>&#8377; " + overdue.toPlainString() + "</td></tr>"
                + "<tr><td class='label'>Late Penalty:</td><td class='value'>&#8377; " + penalty.toPlainString() + "</td></tr>"
                + "<tr><td class='label' style='font-size:16px;'>Total Amount Due:</td><td class='value' style='font-size:16px; color:#b91c1c;'>&#8377; " + total.toPlainString() + "</td></tr>"
                + "</table>"
                + "<div class='btn-container'><a href='" + escapeHtml(paymentUrl) + "' class='btn'>Pay Now Securely</a></div>"
                + "<p>If you have already made this payment in the past few hours, please ignore this notice as reconciliation may be in progress.</p>"
                + "<p>For any queries or assistance, contact our Customer Helpdesk at <strong>" + escapeHtml(helpdesk) + "</strong> or visit your nearest branch.</p>"
                + "<p>Sincerely,<br><strong>Indian Bank Collections & Accounts Team</strong></p>"
                + "</div>"
                + "<div class='footer'>This is an automated communication from Indian Bank Loan Management System. Please do not reply directly to this email.</div>"
                + "</div></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
