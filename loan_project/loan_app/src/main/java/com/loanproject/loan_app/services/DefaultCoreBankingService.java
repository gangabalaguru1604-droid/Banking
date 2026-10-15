package com.loanproject.loan_app.services;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.loanproject.loan_app.dto.SectionResult;

@Service
public class DefaultCoreBankingService implements CoreBankingService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCoreBankingService.class);

    @Value("${bank.cbs.integration-enabled:false}")
    private boolean cbsIntegrationEnabled;

    @Value("${bank.cbs.endpoint-url:}")
    private String cbsEndpointUrl;

    @Override
    public boolean isCbsConfigured() {
        return cbsIntegrationEnabled && cbsEndpointUrl != null && !cbsEndpointUrl.isBlank();
    }

    @Override
    public SectionResult<BigDecimal> getLiveAccountBalance(String accountIdentifier) {
        if (!isCbsConfigured()) {
            log.info("CBS live account balance requested for account ending in {} but CBS interface is unconfigured.",
                    accountIdentifier != null && accountIdentifier.length() > 4 ? accountIdentifier.substring(accountIdentifier.length() - 4) : "N/A");
            return SectionResult.unavailable("Live CBS account balance unavailable: Core Banking System (CBS) interface is unconfigured.");
        }

        // When CBS is configured in production, invoke secure banking API via mTLS/REST
        // Here we cleanly distinguish real CBS from local database records
        return SectionResult.unavailable("Live CBS account balance unavailable: Production CBS connector awaiting operational certificate.");
    }
}
