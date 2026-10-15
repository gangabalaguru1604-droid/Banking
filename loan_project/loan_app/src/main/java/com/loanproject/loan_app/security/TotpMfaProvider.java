package com.loanproject.loan_app.security;

import org.springframework.stereotype.Component;

import com.loanproject.loan_app.bean.User;

@Component
public class TotpMfaProvider implements MfaProvider {

    private final TotpService totpService;
    private final AesGcmEncryptionService encryptionService;

    public TotpMfaProvider(TotpService totpService, AesGcmEncryptionService encryptionService) {
        this.totpService = totpService;
        this.encryptionService = encryptionService;
    }

    @Override
    public String getType() {
        return "TOTP";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void initiate(User user, String challengeToken, String otpCode) {
        // TOTP codes are client-side generated via authenticator app
    }

    @Override
    public boolean verify(User user, String inputCode) {
        if (user == null || user.getMfaSecret() == null || inputCode == null) {
            return false;
        }

        try {
            String decryptedSecret = encryptionService.decrypt(user.getMfaSecret());
            return totpService.verifyCode(decryptedSecret, inputCode);
        } catch (Exception e) {
            return false;
        }
    }
}
