package com.loanproject.loan_app.dto;

import java.io.Serializable;

public class LoanAccountDetailsDTO implements Serializable {

    private Long loanId;
    private String accountNumber;
    private String lastRefreshed;
    private String officerBranch;
    private String userRole;

    private SectionResult<CustomerProfileSectionDTO> customerProfileSection;
    private SectionResult<LoanDetailsSectionDTO> loanDetailsSection;
    private SectionResult<OutstandingSummarySectionDTO> outstandingSummarySection;
    private SectionResult<RepaymentScheduleSectionDTO> repaymentScheduleSection;
    private SectionResult<PaymentHistorySectionDTO> paymentHistorySection;
    private SectionResult<DelinquencySectionDTO> delinquencySection;
    private SectionResult<CollateralGuarantorSectionDTO> collateralGuarantorSection;
    private SectionResult<LinkedAccountSectionDTO> linkedAccountSection;

    public LoanAccountDetailsDTO() {
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getLastRefreshed() {
        return lastRefreshed;
    }

    public void setLastRefreshed(String lastRefreshed) {
        this.lastRefreshed = lastRefreshed;
    }

    public String getOfficerBranch() {
        return officerBranch;
    }

    public void setOfficerBranch(String officerBranch) {
        this.officerBranch = officerBranch;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public SectionResult<CustomerProfileSectionDTO> getCustomerProfileSection() {
        return customerProfileSection;
    }

    public void setCustomerProfileSection(SectionResult<CustomerProfileSectionDTO> customerProfileSection) {
        this.customerProfileSection = customerProfileSection;
    }

    public SectionResult<LoanDetailsSectionDTO> getLoanDetailsSection() {
        return loanDetailsSection;
    }

    public void setLoanDetailsSection(SectionResult<LoanDetailsSectionDTO> loanDetailsSection) {
        this.loanDetailsSection = loanDetailsSection;
    }

    public SectionResult<OutstandingSummarySectionDTO> getOutstandingSummarySection() {
        return outstandingSummarySection;
    }

    public void setOutstandingSummarySection(SectionResult<OutstandingSummarySectionDTO> outstandingSummarySection) {
        this.outstandingSummarySection = outstandingSummarySection;
    }

    public SectionResult<RepaymentScheduleSectionDTO> getRepaymentScheduleSection() {
        return repaymentScheduleSection;
    }

    public void setRepaymentScheduleSection(SectionResult<RepaymentScheduleSectionDTO> repaymentScheduleSection) {
        this.repaymentScheduleSection = repaymentScheduleSection;
    }

    public SectionResult<PaymentHistorySectionDTO> getPaymentHistorySection() {
        return paymentHistorySection;
    }

    public void setPaymentHistorySection(SectionResult<PaymentHistorySectionDTO> paymentHistorySection) {
        this.paymentHistorySection = paymentHistorySection;
    }

    public SectionResult<DelinquencySectionDTO> getDelinquencySection() {
        return delinquencySection;
    }

    public void setDelinquencySection(SectionResult<DelinquencySectionDTO> delinquencySection) {
        this.delinquencySection = delinquencySection;
    }

    public SectionResult<CollateralGuarantorSectionDTO> getCollateralGuarantorSection() {
        return collateralGuarantorSection;
    }

    public void setCollateralGuarantorSection(SectionResult<CollateralGuarantorSectionDTO> collateralGuarantorSection) {
        this.collateralGuarantorSection = collateralGuarantorSection;
    }

    private String branch;
    private String correlationId;

    public String getBranch() {
        return branch != null ? branch : officerBranch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCorrelationId() {
        return correlationId != null ? correlationId : "CORR-" + (loanId != null ? loanId : "0");
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getLastRefreshedFormatted() {
        return lastRefreshed != null ? lastRefreshed : "N/A";
    }

    // Convenient aliases matching both JSP and PDF generator conventions
    public SectionResult<CustomerProfileSectionDTO> getCustomerProfile() {
        return customerProfileSection;
    }

    public SectionResult<LoanDetailsSectionDTO> getLoanDetails() {
        return loanDetailsSection;
    }

    public SectionResult<OutstandingSummarySectionDTO> getOutstandingSummary() {
        return outstandingSummarySection;
    }

    public SectionResult<RepaymentScheduleSectionDTO> getRepaymentSchedule() {
        return repaymentScheduleSection;
    }

    public SectionResult<PaymentHistorySectionDTO> getPaymentHistory() {
        return paymentHistorySection;
    }

    public SectionResult<DelinquencySectionDTO> getDelinquency() {
        return delinquencySection;
    }

    public SectionResult<CollateralGuarantorSectionDTO> getCollateralGuarantor() {
        return collateralGuarantorSection;
    }

    public SectionResult<LinkedAccountSectionDTO> getLinkedAccount() {
        return linkedAccountSection;
    }

    public SectionResult<LinkedAccountSectionDTO> getLinkedAccountSection() {
        return linkedAccountSection;
    }

    public void setLinkedAccountSection(SectionResult<LinkedAccountSectionDTO> linkedAccountSection) {
        this.linkedAccountSection = linkedAccountSection;
    }
}
