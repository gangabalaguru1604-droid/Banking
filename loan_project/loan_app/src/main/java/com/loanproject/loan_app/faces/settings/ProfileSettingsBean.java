package com.loanproject.loan_app.faces.settings;

import java.io.Serializable;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.CustomUserDetails;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

@Component("profileSettingsBean")
@SessionScope
public class ProfileSettingsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ProfileSettingsBean.class);

    private final UserRepository userRepository;
    private final AuthAuditService auditService;

    // Read-only system and security attributes
    private Long userId;
    private String customerId;
    private String email;
    private AppRole role;
    private String branch;
    private String kycStatus;
    private boolean accountNonLocked;

    // Permitted editable fields
    private String name;
    private String phoneNumber;
    private String address;
    private String preferredContactMethod = "EMAIL";
    private LocalDate dateOfBirth;

    // View feedback
    private String successMessage;
    private String errorMessage;

    public ProfileSettingsBean(UserRepository userRepository, AuthAuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        loadUserProfile();
    }

    public void loadUserProfile() {
        this.successMessage = null;
        this.errorMessage = null;

        User user = getAuthenticatedUser();
        if (user == null) {
            log.warn("ProfileSettingsBean: No authenticated user found in SecurityContext");
            return;
        }

        // Re-read latest record from DB
        user = userRepository.findById(user.getId()).orElse(user);

        this.userId = user.getId();
        this.customerId = user.getCustomerId() != null ? user.getCustomerId() : ("CUST" + String.format("%06d", user.getId()));
        this.email = user.getEmail();
        this.role = user.getRole();
        this.branch = user.getBranch();
        this.kycStatus = user.getKycStatus() != null ? user.getKycStatus() : "VERIFIED";
        this.accountNonLocked = user.isAccountNonLocked();

        // Editable fields
        this.name = user.getName();
        this.phoneNumber = user.getPhoneNumber();
        this.address = user.getAddress();
        this.preferredContactMethod = user.getPreferredContactMethod() != null ? user.getPreferredContactMethod() : "EMAIL";
        this.dateOfBirth = user.getDateOfBirth();
    }

    public void saveProfile() {
        this.successMessage = null;
        this.errorMessage = null;

        User user = getAuthenticatedUser();
        if (user == null) {
            this.errorMessage = "Session expired or user not authenticated. Please log in.";
            return;
        }

        User dbUser = userRepository.findById(user.getId()).orElse(null);
        if (dbUser == null) {
            this.errorMessage = "User account not found in system.";
            return;
        }

        // Server-side validation
        if (this.name == null || this.name.trim().length() < 2 || this.name.trim().length() > 100) {
            this.errorMessage = "Full Name is required and must be between 2 and 100 characters.";
            return;
        }

        if (this.phoneNumber != null && !this.phoneNumber.trim().isEmpty()) {
            String cleanPhone = this.phoneNumber.trim().replaceAll("[^0-9]", "");
            if (cleanPhone.length() != 10) {
                this.errorMessage = "Phone number must be a valid 10-digit Indian mobile number.";
                return;
            }
            dbUser.setPhoneNumber(cleanPhone);
            this.phoneNumber = cleanPhone;
        } else {
            dbUser.setPhoneNumber(null);
        }

        if (this.address != null && this.address.length() > 255) {
            this.errorMessage = "Address cannot exceed 255 characters.";
            return;
        }

        // Apply permitted changes ONLY (Identity, Role, Branch & CustomerId are strictly immutable)
        dbUser.setName(this.name.trim());
        dbUser.setAddress(this.address != null ? this.address.trim() : null);
        dbUser.setPreferredContactMethod(this.preferredContactMethod != null ? this.preferredContactMethod : "EMAIL");
        dbUser.setDateOfBirth(this.dateOfBirth);

        userRepository.save(dbUser);

        // Security audit logging
        HttpServletRequest req = getHttpServletRequest();
        auditService.logProfileUpdated(dbUser.getEmail(), req, "Name=" + dbUser.getName() + ", Phone=" + dbUser.getPhoneNumber() + ", Contact=" + dbUser.getPreferredContactMethod());

        this.successMessage = "Profile updated successfully!";
        log.info("Profile updated for user: {}", dbUser.getEmail());
    }

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }
        if (auth.getName() != null) {
            return userRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
        }
        return null;
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null && ctx.getExternalContext() != null) {
                Object req = ctx.getExternalContext().getRequest();
                if (req instanceof HttpServletRequest httpReq) {
                    return httpReq;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public String getCustomerId() { return customerId; }
    public String getEmail() { return email; }
    public AppRole getRole() { return role; }
    public String getBranch() { return branch; }
    public String getKycStatus() { return kycStatus; }
    public boolean isAccountNonLocked() { return accountNonLocked; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(String preferredContactMethod) { this.preferredContactMethod = preferredContactMethod; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
