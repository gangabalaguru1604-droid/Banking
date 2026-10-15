package com.loanproject.loan_app.services;

import com.loanproject.loan_app.dto.ReminderMessageRequest;
import com.loanproject.loan_app.dto.ReminderSendResult;

public interface EmailReminderProvider {

    /**
     * Sends the appropriate DPD reminder email.
     *
     * @param request the reminder details
     * @return the provider send result including providerMessageId if accepted
     */
    ReminderSendResult sendReminderEmail(ReminderMessageRequest request);
}
