package com.loanproject.loan_app.bean;

@org.springframework.data.mongodb.core.mapping.Document(collection = "guarantors")

public class Guarantor {

    @org.springframework.data.annotation.Id

    private Long id;

    @org.springframework.data.mongodb.core.mapping.DBRef

    private Loan loan;

    private String name;

    private String relationship;

    private String phone;

    private String email;

    private String pan;

    public Guarantor() {
    }

    public Guarantor(Loan loan, String name, String relationship, String phone, String email, String pan) {
        this.loan = loan;
        this.name = name;
        this.relationship = relationship;
        this.phone = phone;
        this.email = email;
        this.pan = pan;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }
}
