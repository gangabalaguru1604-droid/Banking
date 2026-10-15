package com.loanproject.loan_app.bean;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "collaterals")
public class Collateral {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Loan loan;

    @Column(nullable = false, length = 100)
    private String collateralType; // e.g. Real Estate, Vehicle, Fixed Deposit, Gold

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal collateralValue;

    @Column(length = 255)
    private String description;

    @Column(length = 30)
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
