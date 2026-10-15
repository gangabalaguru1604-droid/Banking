package com.loanproject.loan_app.security;

import com.loanproject.loan_app.bean.User;

public interface MfaProvider {

    String getType();

    boolean isAvailable();

    void initiate(User user, String challengeToken, String otpCode);

    boolean verify(User user, String inputCode);
}
