package com.loanproject.loan_app.services;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
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

import com.loanproject.loan_app.dto.ReminderMessageRequest;
import com.loanproject.loan_app.dto.ReminderSendResult;

import jakarta.mail.internet.MimeMessage;

@Service
public class DefaultEmailReminderProvider implements EmailReminderProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmailReminderProvider.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${reminder.mail.from:reminders@patilcapitals.com}")
    private String fromEmail;

    @Value("${reminder.mail.simulate:false}")
    private boolean simulateSend;

    @Override
    public ReminderSendResult sendReminderEmail(ReminderMessageRequest request) {
        if (request == null) {
            return ReminderSendResult.failure("Request must not be null", false);
        }
        if (request.getRecipientEmail() == null || request.getRecipientEmail().isBlank()) {
            return ReminderSendResult.failure("Recipient email is empty", false);
        }

        String subject = buildSubject(request);
        String body = buildHtmlBody(request);

        // If mailSender is not configured or simulation is turned on, simulate provider acceptance safely
        if (mailSender == null || simulateSend) {
            String mockMessageId = "EML-SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("Simulated email dispatch to {} (Message ID: {}): {}",
                    request.getRecipientEmail(), mockMessageId, subject);
            return ReminderSendResult.success(mockMessageId);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(request.getRecipientEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            String providerMessageId = "EML-" + UUID.randomUUID().toString();
            message.setHeader("X-Message-ID", providerMessageId);
            message.setHeader("X-Loan-Account", request.getMaskedAccountNumber());
            message.setHeader("X-DPD-Day", String.valueOf(request.getDpdDay()));

            mailSender.send(message);
            log.info("Email sent successfully to {} with Provider ID {}", request.getRecipientEmail(), providerMessageId);
            return ReminderSendResult.success(providerMessageId);
        } catch (MailAuthenticationException ex) {
            log.error("Authentication failure sending reminder email: {}", ex.getMessage());
            return ReminderSendResult.failure("Email authentication failed: " + ex.getMessage(), false);
        } catch (MailException ex) {
            log.warn("Transient failure sending reminder email to {}: {}", request.getRecipientEmail(), ex.getMessage());
            return ReminderSendResult.failure("Email dispatch transient failure: " + ex.getMessage(), true);
        } catch (Exception ex) {
            log.error("Unexpected error sending reminder email: {}", ex.getMessage(), ex);
            return ReminderSendResult.failure("Email unexpected failure: " + ex.getMessage(), false);
        }
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
                + "<p>Sincerely,<br><strong>Patil Capitals Collections & Accounts Team</strong></p>"
                + "</div>"
                + "<div class='footer'>This is an automated communication from Patil Capitals Loan Management System. Please do not reply directly to this email.</div>"
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
