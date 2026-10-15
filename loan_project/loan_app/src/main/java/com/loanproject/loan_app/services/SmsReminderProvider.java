package com.loanproject.loan_app.services;

import com.loanproject.loan_app.dto.ReminderMessageRequest;
import com.loanproject.loan_app.dto.ReminderSendResult;

public interface SmsReminderProvider {

    /**
     * Sends the fallback DPD reminder SMS.
     *
     * @param request the reminder details
     * @return the provider send result including providerMessageId if accepted
     */
    ReminderSendResult sendReminderSms(ReminderMessageRequest request);
}
