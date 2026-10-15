package com.loanproject.loan_app.security;

import java.util.Optional;

import com.loanproject.loan_app.bean.User;

public interface BankIdentityProvider {

    boolean isProductionProviderConfigured();

    String getProviderName();

    Optional<User> authenticateStaff(String staffIdOrEmail, String rawPassword);
}
