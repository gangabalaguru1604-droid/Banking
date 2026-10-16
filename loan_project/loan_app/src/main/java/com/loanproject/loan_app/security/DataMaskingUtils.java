package com.loanproject.loan_app.security;

public final class DataMaskingUtils {

    private DataMaskingUtils() {
    }

    /**
     * Masks an Aadhaar number to standard format XXXX-XXXX-1234 (only last 4 digits exposed).
     */
    public static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.isBlank()) {
            return "Not Provided";
        }
        String clean = aadhaar.replaceAll("[^0-9]", "");
        if (clean.length() >= 4) {
            String last4 = clean.substring(clean.length() - 4);
            return "XXXX-XXXX-" + last4;
        }
        return "XXXX-XXXX-XXXX";
    }

    /**
     * Masks a PAN card number to ABCDE****F (first 5 and last character visible, middle 4 masked).
     */
    public static String maskPan(String pan) {
        if (pan == null || pan.isBlank()) {
            return "Not Provided";
        }
        String clean = pan.trim().toUpperCase();
        if (clean.length() == 10) {
            return clean.substring(0, 5) + "****" + clean.charAt(9);
        } else if (clean.length() > 4) {
            return clean.substring(0, 2) + "****" + clean.substring(clean.length() - 2);
        }
        return "**********";
    }

    /**
     * Masks a mobile number to ******1234 (only last 4 digits exposed).
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "Not Provided";
        }
        String clean = phone.replaceAll("[^0-9]", "");
        if (clean.length() >= 4) {
            String last4 = clean.substring(clean.length() - 4);
            return "******" + last4;
        }
        return "**********";
    }

    /**
     * Masks an email address to g***a@domain.com.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "Not Provided";
        }
        String clean = email.trim();
        int atIndex = clean.indexOf('@');
        String username = clean.substring(0, atIndex);
        String domain = clean.substring(atIndex);

        if (username.length() <= 2) {
            return username.charAt(0) + "***" + domain;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + domain;
    }

    /**
     * Alias for maskPhone for consistent API usage.
     */
    public static String maskPhoneNumber(String phone) {
        return maskPhone(phone);
    }

    /**
     * Masks a loan account number, e.g. LN0000000001 -> LN******0001.
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "******";
        }
        String clean = accountNumber.trim();
        if (clean.length() <= 4) {
            return "******" + clean;
        }
        if (clean.startsWith("LN") && clean.length() > 6) {
            return "LN******" + clean.substring(clean.length() - 4);
        }
        return clean.substring(0, 2) + "******" + clean.substring(clean.length() - 4);
    }

    /**
     * Sanitizes URLs before logging so sensitive query parameters (account, token, key, phone, etc.) are masked.
     */
    public static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        int queryIndex = url.indexOf('?');
        if (queryIndex < 0 || queryIndex == url.length() - 1) {
            return url;
        }
        String baseUrl = url.substring(0, queryIndex);
        String queryString = url.substring(queryIndex + 1);
        String[] pairs = queryString.split("&");
        StringBuilder sanitizedQuery = new StringBuilder();

        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                sanitizedQuery.append("&");
            }
            String pair = pairs[i];
            int eqIndex = pair.indexOf('=');
            if (eqIndex < 0) {
                sanitizedQuery.append(pair);
                continue;
            }
            String key = pair.substring(0, eqIndex);
            String val = pair.substring(eqIndex + 1);
            String lowerKey = key.toLowerCase();

            if (lowerKey.contains("account")) {
                sanitizedQuery.append(key).append("=").append(maskAccountNumber(val));
            } else if (lowerKey.contains("phone") || lowerKey.contains("mobile")) {
                sanitizedQuery.append(key).append("=").append(maskPhone(val));
            } else if (lowerKey.contains("email")) {
                sanitizedQuery.append(key).append("=").append(maskEmail(val));
            } else if (lowerKey.contains("token") || lowerKey.contains("key") || lowerKey.contains("secret") || lowerKey.contains("pass")) {
                sanitizedQuery.append(key).append("=******");
            } else {
                sanitizedQuery.append(pair);
            }
        }
        return baseUrl + "?" + sanitizedQuery.toString();
    }
}
