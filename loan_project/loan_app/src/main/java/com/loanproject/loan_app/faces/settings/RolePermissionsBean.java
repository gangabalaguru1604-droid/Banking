package com.loanproject.loan_app.faces.settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.UserService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

@Component("rolePermissionsBean")
@SessionScope
public class RolePermissionsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(RolePermissionsBean.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthAuditService auditService;

    // View data
    private List<RoleDefinitionDto> roleDefinitions = new ArrayList<>();
    private List<User> staffList = new ArrayList<>();
    private Long selectedStaffId;
    private AppRole selectedNewRole;

    // Feedback
    private String successMessage;
    private String errorMessage;

    public RolePermissionsBean(UserService userService,
                               UserRepository userRepository,
                               AuthAuditService auditService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        buildRoleDefinitions();
        loadStaffList();
    }

    public void buildRoleDefinitions() {
        this.roleDefinitions = List.of(
            new RoleDefinitionDto(
                AppRole.ROLE_CUSTOMER,
                "Customer / Borrower",
                "Self-service portal user applying for and servicing personal bank loans.",
                "Own Accounts Only",
                List.of(
                    "Submit Loan Applications (/applyLoan)",
                    "View Personal Dashboard & Loan Accounts (/dashboard)",
                    "Make Online EMI Repayments (/payPage, /pay)",
                    "Download Statement of Account (PDF)",
                    "Manage Personal Profile & Security Settings"
                ),
                List.of(
                    "Cannot view other customer loans",
                    "Cannot approve or reject loans",
                    "Cannot access staff or admin portals",
                    "Cannot manage staff roles"
                )
            ),
            new RoleDefinitionDto(
                AppRole.ROLE_LOAN_OFFICER,
                "Loan Underwriting Officer",
                "Branch credit officer responsible for reviewing, verifying, and sanctioning loan applications.",
                "Assigned Branch Only",
                List.of(
                    "Access JSF Loan Approval Dashboard (/loan-approval.xhtml)",
                    "Review Borrower Details & Verification Data",
                    "Approve Eligible Pending Applications & Generate Schedules",
                    "Reject Ineligible Applications with Mandatory Justification",
                    "Originate Customer Loans at Branch",
                    "Perform 360-Degree Borrower Search (/loan-search)"
                ),
                List.of(
                    "Cannot decide loans outside assigned branch",
                    "Cannot process customer repayments",
                    "Cannot modify staff roles or system settings"
                )
            ),
            new RoleDefinitionDto(
                AppRole.ROLE_COLLECTIONS_AGENT,
                "Collections Field Agent",
                "Field recovery agent managing delinquent borrower accounts, reminders, and payment receipts.",
                "Assigned Branch Only",
                List.of(
                    "View Branch Delinquency & DPD Buckets (SMA-0, SMA-1, SMA-2, NPA)",
                    "Perform Borrower 360 Searches & Account Details",
                    "Record In-Person / Physical Collection Receipts",
                    "Monitor Automated DPD Reminders & Dispatch Logs"
                ),
                List.of(
                    "Cannot originate or apply for loans",
                    "Cannot approve or reject loan applications",
                    "Cannot modify system policies or user roles"
                )
            ),
            new RoleDefinitionDto(
                AppRole.ROLE_COLLECTIONS_MANAGER,
                "Collections Supervisor",
                "Supervisory officer monitoring portfolio-at-risk (PAR), recovery metrics, and collections operations.",
                "Assigned Branch Only",
                List.of(
                    "Oversee Full Branch Delinquency Portfolio & Recovery Targets",
                    "Analyze DPD Aging Buckets & Recovery Success Rates",
                    "View Detailed Borrower KYC and Collection History",
                    "Coordinate Reminder Dispatch Schedules"
                ),
                List.of(
                    "Cannot originate or apply for loans",
                    "Cannot approve or reject loan applications",
                    "Cannot modify system policies or user roles"
                )
            ),
            new RoleDefinitionDto(
                AppRole.ROLE_ADMIN,
                "System Administrator",
                "Central banking administrator managing staff provisioning, audit trails, and global operations.",
                "All Branches (Global Access)",
                List.of(
                    "Global Staff Provisioning & Role Management (/admin/staff)",
                    "Approve or Reject Loans Across All Bank Branches",
                    "Inspect Immutable System Audit Trails (/admin/audit-logs)",
                    "Configure Bank Underwriting Policies & Limits",
                    "Manage System Diagnostics & Operational Parameters"
                ),
                List.of(
                    "All administrative operations are strictly logged in audit trail"
                )
            )
        );
    }

    public void loadStaffList() {
        try {
            this.staffList = userService.findAllStaff();
        } catch (Exception ex) {
            log.error("Failed to load staff list: {}", ex.getMessage());
            this.staffList = new ArrayList<>();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void updateStaffRole() {
        this.successMessage = null;
        this.errorMessage = null;

        // Server-side strict authorization check
        User currentUser = getAuthenticatedUser();
        if (currentUser == null || currentUser.getRole() != AppRole.ROLE_ADMIN) {
            this.errorMessage = "Access Denied: Only administrators are authorized to update staff roles.";
            throw new AccessDeniedException(this.errorMessage);
        }

        if (this.selectedStaffId == null) {
            this.errorMessage = "Please select a staff member to update.";
            return;
        }

        if (this.selectedNewRole == null || this.selectedNewRole == AppRole.ROLE_CUSTOMER) {
            this.errorMessage = "Staff members must be assigned a valid staff or administrator role.";
            return;
        }

        User targetStaff = userRepository.findById(this.selectedStaffId).orElse(null);
        if (targetStaff == null) {
            this.errorMessage = "Staff member not found with ID: " + this.selectedStaffId;
            return;
        }

        AppRole oldRole = targetStaff.getRole();
        userService.updateStaffRole(this.selectedStaffId, this.selectedNewRole);

        // Security audit logging
        HttpServletRequest req = getHttpServletRequest();
        auditService.logRoleChanged(currentUser.getEmail(), req, targetStaff.getEmail(), this.selectedNewRole.name());

        this.successMessage = "Role for staff member " + targetStaff.getName() + " (" + targetStaff.getEmail()
                + ") successfully updated from " + oldRole.getDisplayName() + " to " + this.selectedNewRole.getDisplayName() + "!";

        log.info("Admin {} updated staff {} role from {} to {}", currentUser.getEmail(), targetStaff.getEmail(), oldRole, this.selectedNewRole);

        loadStaffList();
        this.selectedStaffId = null;
        this.selectedNewRole = null;
    }

    public boolean isUserAdmin() {
        User user = getAuthenticatedUser();
        return user != null && user.getRole() == AppRole.ROLE_ADMIN;
    }

    public List<AppRole> getAvailableStaffRoles() {
        return List.of(
            AppRole.ROLE_LOAN_OFFICER,
            AppRole.ROLE_COLLECTIONS_AGENT,
            AppRole.ROLE_COLLECTIONS_MANAGER,
            AppRole.ROLE_ADMIN
        );
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

    // DTO for role matrix presentation
    public static class RoleDefinitionDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private final AppRole role;
        private final String title;
        private final String summary;
        private final String branchScope;
        private final List<String> permissions;
        private final List<String> restrictions;

        public RoleDefinitionDto(AppRole role, String title, String summary, String branchScope,
                                 List<String> permissions, List<String> restrictions) {
            this.role = role;
            this.title = title;
            this.summary = summary;
            this.branchScope = branchScope;
            this.permissions = permissions;
            this.restrictions = restrictions;
        }

        public AppRole getRole() { return role; }
        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public String getBranchScope() { return branchScope; }
        public List<String> getPermissions() { return permissions; }
        public List<String> getRestrictions() { return restrictions; }
    }

    // Getters and Setters
    public List<RoleDefinitionDto> getRoleDefinitions() { return roleDefinitions; }
    public List<User> getStaffList() { return staffList; }

    public Long getSelectedStaffId() { return selectedStaffId; }
    public void setSelectedStaffId(Long selectedStaffId) { this.selectedStaffId = selectedStaffId; }

    public AppRole getSelectedNewRole() { return selectedNewRole; }
    public void setSelectedNewRole(AppRole selectedNewRole) { this.selectedNewRole = selectedNewRole; }

    public String getSuccessMessage() { return successMessage; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
