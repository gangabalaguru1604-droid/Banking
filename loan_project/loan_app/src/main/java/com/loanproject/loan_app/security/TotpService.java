package com.loanproject.loan_app.security;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int MODULO = 1_000_000;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a new random Base32 shared secret (160 bits / 20 bytes).
     */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /**
     * Verifies a 6-digit TOTP code against the secret at the current time with +/- 1 time step window tolerance.
     */
    public boolean verifyCode(String base32Secret, String inputCode) {
        if (base32Secret == null || inputCode == null || inputCode.length() != CODE_DIGITS) {
            return false;
        }

        try {
            int code = Integer.parseInt(inputCode.trim());
            long currentStep = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;

            // Allow window of +/- 1 time-step for clock drift
            for (int i = -1; i <= 1; i++) {
                long step = currentStep + i;
                int calculatedCode = generateTotpForStep(base32Secret, step);
                if (calculatedCode == code) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    /**
     * Generates TOTP code for the current time (used in tests or authenticator sync).
     */
    public String generateCurrentCode(String base32Secret) {
        long currentStep = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        int code = generateTotpForStep(base32Secret, currentStep);
        return String.format("%06d", code);
    }

    public int generateTotpForStep(String base32Secret, long step) {
        try {
            byte[] key = decodeBase32(base32Secret);
            byte[] data = ByteBuffer.allocate(8).putLong(step).array();

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            // Dynamic truncation (RFC 4226)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return binary % MODULO;
        } catch (Exception e) {
            throw new RuntimeException("Error computing TOTP", e);
        }
    }

    public String getOtpAuthUrl(String accountName, String issuer, String base32Secret) {
        String encodedIssuer = java.net.URLEncoder.encode(issuer, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        String encodedAccount = java.net.URLEncoder.encode(accountName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                encodedIssuer, encodedAccount, base32Secret, encodedIssuer, CODE_DIGITS, TIME_STEP_SECONDS);
    }

    /**
     * Formats a Base32 secret for user-friendly manual entry (grouped into 4-character chunks).
     */
    public String formatSecretForDisplay(String secret) {
        if (secret == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < secret.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append(" ");
            }
            sb.append(secret.charAt(i));
        }
        return sb.toString();
    }

    // Standard Base32 encoding (RFC 4648)
    public static String encodeBase32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                out.append(BASE32_ALPHABET.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }

        if (bitsLeft > 0) {
            buffer <<= (5 - bitsLeft);
            out.append(BASE32_ALPHABET.charAt(buffer & 0x1F));
        }

        return out.toString();
    }

    // Standard Base32 decoding (RFC 4648)
    public static byte[] decodeBase32(String base32) {
        String clean = base32.trim().toUpperCase().replace(" ", "").replace("-", "");
        byte[] bytes = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;

        for (char c : clean.toCharArray()) {
            if (c == '=') break; // padding
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Illegal character in Base32 string: " + c);
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                bytes[count++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }

        return Arrays.copyOf(bytes, count);
    }
}
