package com.loanproject.loan_app.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CollateralGuarantorSectionDTO implements Serializable {

    private List<CollateralItemDTO> collaterals = new ArrayList<>();
    private List<GuarantorItemDTO> guarantors = new ArrayList<>();

    public CollateralGuarantorSectionDTO() {
    }

    public CollateralGuarantorSectionDTO(List<CollateralItemDTO> collaterals, List<GuarantorItemDTO> guarantors) {
        this.collaterals = collaterals != null ? collaterals : new ArrayList<>();
        this.guarantors = guarantors != null ? guarantors : new ArrayList<>();
    }

    public List<CollateralItemDTO> getCollaterals() {
        return collaterals;
    }

    public void setCollaterals(List<CollateralItemDTO> collaterals) {
        this.collaterals = collaterals;
    }

    public List<GuarantorItemDTO> getGuarantors() {
        return guarantors;
    }

    public void setGuarantors(List<GuarantorItemDTO> guarantors) {
        this.guarantors = guarantors;
    }

    public static class CollateralItemDTO implements Serializable {
        private String collateralType;
        private BigDecimal collateralValue;
        private String description;
        private String status;

        public CollateralItemDTO() {
        }

        public CollateralItemDTO(String collateralType, BigDecimal collateralValue, String description, String status) {
            this.collateralType = collateralType;
            this.collateralValue = collateralValue;
            this.description = description;
            this.status = status;
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

    public static class GuarantorItemDTO implements Serializable {
        private String name;
        private String relationship;
        private String maskedPhone;
        private String maskedEmail;
        private String maskedPan;

        public GuarantorItemDTO() {
        }

        public GuarantorItemDTO(String name, String relationship, String maskedPhone, String maskedEmail, String maskedPan) {
            this.name = name;
            this.relationship = relationship;
            this.maskedPhone = maskedPhone;
            this.maskedEmail = maskedEmail;
            this.maskedPan = maskedPan;
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

        public String getMaskedPhone() {
            return maskedPhone;
        }

        public void setMaskedPhone(String maskedPhone) {
            this.maskedPhone = maskedPhone;
        }

        public String getMaskedEmail() {
            return maskedEmail;
        }

        public void setMaskedEmail(String maskedEmail) {
            this.maskedEmail = maskedEmail;
        }

        public String getMaskedPan() {
            return maskedPan;
        }

        public void setMaskedPan(String maskedPan) {
            this.maskedPan = maskedPan;
        }
    }
}
