package com.loanproject.loan_app.bean;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_approval_history")
public class LoanApprovalHistory {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(nullable = false, length = 30)
    private String action; // APPROVED, REJECTED

    @Column(length = 30)
    private String previousStatus;

    @Column(nullable = false, length = 30)
    private String newStatus;

    @Column(nullable = false, length = 100)
    private String reviewerEmail;

    @Column(length = 100)
    private String reviewerName;

    @Column(length = 50)
    private String reviewerRole;

    @Column(length = 1000)
    private String comments;

    @Column(length = 50)
    private String branch;

    @Column(nullable = false)
    private Instant decisionTimestamp = Instant.now();

    public LoanApprovalHistory() {
    }

    public LoanApprovalHistory(Loan loan, String action, String previousStatus, String newStatus,
                               String reviewerEmail, String reviewerName, String reviewerRole,
                               String comments, String branch) {
        this.loan = loan;
        this.action = action;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reviewerEmail = reviewerEmail;
        this.reviewerName = reviewerName;
        this.reviewerRole = reviewerRole;
        this.comments = comments;
        this.branch = branch;
        this.decisionTimestamp = Instant.now();
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getReviewerEmail() {
        return reviewerEmail;
    }

    public void setReviewerEmail(String reviewerEmail) {
        this.reviewerEmail = reviewerEmail;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerRole() {
        return reviewerRole;
    }

    public void setReviewerRole(String reviewerRole) {
        this.reviewerRole = reviewerRole;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Instant getDecisionTimestamp() {
        return decisionTimestamp;
    }

    public void setDecisionTimestamp(Instant decisionTimestamp) {
        this.decisionTimestamp = decisionTimestamp;
    }
}
