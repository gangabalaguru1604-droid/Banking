package com.loanproject.loan_app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CustomerActivityDto {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
            .withZone(ZoneId.systemDefault());

    private String type;
    private String title;
    private String description;
    private Instant timestamp;
    private String formattedDate;
    private String status;
    private BigDecimal amount;
    private String badgeClass;
    private String icon;

    public CustomerActivityDto() {
    }

    public CustomerActivityDto(String type, String title, String description, Instant timestamp,
                               String status, BigDecimal amount, String badgeClass, String icon) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.formattedDate = DATE_FORMATTER.format(this.timestamp);
        this.status = status;
        this.amount = amount;
        this.badgeClass = badgeClass;
        this.icon = icon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        if (timestamp != null) {
            this.formattedDate = DATE_FORMATTER.format(timestamp);
        }
    }

    public String getFormattedDate() {
        if (formattedDate == null && timestamp != null) {
            formattedDate = DATE_FORMATTER.format(timestamp);
        }
        return formattedDate != null ? formattedDate : "";
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public void setBadgeClass(String badgeClass) {
        this.badgeClass = badgeClass;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
