package com.loanproject.loan_app.bean;

public enum AppRole {
    ROLE_CUSTOMER("Customer"),
    ROLE_LOAN_OFFICER("Loan Officer"),
    ROLE_COLLECTIONS_AGENT("Collections Agent"),
    ROLE_COLLECTIONS_MANAGER("Collections Manager"),
    ROLE_ADMIN("Administrator");

    private final String displayName;

    AppRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
