package com.loanproject.loan_app.services;

import java.math.BigDecimal;

import com.loanproject.loan_app.dto.SectionResult;

public interface CoreBankingService {

    /**
     * Checks whether a live external Core Banking System (CBS) integration is configured.
     */
    boolean isCbsConfigured();

    /**
     * Retrieves the live balance for a linked account from the Core Banking System.
     * When unconfigured, returns an informative UNAVAILABLE SectionResult without fabricating balances.
     */
    SectionResult<BigDecimal> getLiveAccountBalance(String accountIdentifier);
}
