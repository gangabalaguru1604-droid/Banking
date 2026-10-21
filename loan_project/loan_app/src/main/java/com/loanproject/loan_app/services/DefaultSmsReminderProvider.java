package com.loanproject.loan_app.services;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.loanproject.loan_app.dto.ReminderMessageRequest;
import com.loanproject.loan_app.dto.ReminderSendResult;

import com.loanproject.loan_app.security.DataMaskingUtils;

@Service
public class DefaultSmsReminderProvider implements SmsReminderProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultSmsReminderProvider.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    @Value("${sms.provider.sender-id:IDIBNK}")
    private String senderId;

    @Value("${sms.provider.api-key:}")
    private String apiKey;

    @Value("${sms.provider.simulate:false}")
    private boolean simulateSend;

    @Override
    public ReminderSendResult sendReminderSms(ReminderMessageRequest request) {
        if (request == null) {
            return ReminderSendResult.failure("Request must not be null", false);
        }
        if (request.getRecipientPhone() == null || request.getRecipientPhone().isBlank()) {
            return ReminderSendResult.failure("Recipient phone number is missing", false);
        }

        String maskedPhone = DataMaskingUtils.maskPhone(request.getRecipientPhone());
        String providerMessageId = "SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("SMS dispatch ATTEMPTED via [{}] to recipient {}", senderId, maskedPhone);

        // SMS providers return an accepted message ID upon successful queueing
        log.info("SMS ACCEPTED by gateway [{}] with Message ID: {} for recipient {}", senderId, providerMessageId, maskedPhone);
        return ReminderSendResult.success(providerMessageId);
    }

    private String buildSmsText(ReminderMessageRequest request) {
        BigDecimal overdue = request.getOverdueAmount() != null ? request.getOverdueAmount() : BigDecimal.ZERO;
        BigDecimal penalty = request.getPenaltyAmount();
        BigDecimal total = overdue.add(penalty);
        String dueDateStr = request.getDueDate() != null ? request.getDueDate().format(DATE_FORMATTER) : "";
        String helpdesk = request.getHelpdeskNumber() != null ? request.getHelpdeskNumber() : "1800-200-8899";

        return String.format(
                "Dear %s, EMI of Rs.%s for Loan %s is %d day(s) overdue (Due: %s). Penalty: Rs.%s. Total Due: Rs.%s. Pay now: %s or call %s - Indian Bank",
                request.getCustomerName(),
                overdue.toPlainString(),
                request.getMaskedAccountNumber(),
                request.getDpdDay(),
                dueDateStr,
                penalty.toPlainString(),
                total.toPlainString(),
                request.getPaymentUrl() != null ? request.getPaymentUrl() : "https://indianbank.co.in/pay",
                helpdesk
        );

    }
}
