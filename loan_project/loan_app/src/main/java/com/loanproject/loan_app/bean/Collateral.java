package com.loanproject.loan_app.bean;

import java.math.BigDecimal;

@org.springframework.data.mongodb.core.mapping.Document(collection = "collaterals")

public class Collateral {

    @org.springframework.data.annotation.Id

    private Long id;

    @org.springframework.data.mongodb.core.mapping.DBRef

    private Loan loan;

    private String collateralType; // e.g. Real Estate, Vehicle, Fixed Deposit, Gold

    private BigDecimal collateralValue;

    private String description;

    private String status = "PLEDGED"; // PLEDGED, RELEASED, INVOKED

    public Collateral() {
    }

    public Collateral(Loan loan, String collateralType, BigDecimal collateralValue, String description, String status) {
        this.loan = loan;
        this.collateralType = collateralType;
        this.collateralValue = collateralValue;
        this.description = description;
        this.status = status != null ? status : "PLEDGED";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public String getCollateralType() {
        return collateralType;
    }

    public void setCollateralType(String collateralType) {
        this.collateralType = collateralType;
    }

    public BigDecimal getCollateralValue() {
        return collateralValue;
    }

    public void setCollateralValue(BigDecimal collateralValue) {
        this.collateralValue = collateralValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
