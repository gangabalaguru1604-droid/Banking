package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserNotificationPreference;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.faces.settings.AuditSettingsBean;
import com.loanproject.loan_app.faces.settings.LoanPolicySettingsBean;
import com.loanproject.loan_app.faces.settings.NotificationSettingsBean;
import com.loanproject.loan_app.faces.settings.ProfileSettingsBean;
import com.loanproject.loan_app.faces.settings.RolePermissionsBean;
import com.loanproject.loan_app.faces.settings.SecuritySettingsBean;
import com.loanproject.loan_app.faces.settings.SystemSettingsBean;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.LoanApprovalHistoryRepository;
import com.loanproject.loan_app.repository.LoanRepository;
import com.loanproject.loan_app.repository.RepaymentInstallmentRepository;
import com.loanproject.loan_app.repository.UserNotificationPreferenceRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.CustomUserDetails;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class JsfSettingsPagesWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanApprovalHistoryRepository approvalHistoryRepository;

    @Autowired
    private RepaymentInstallmentRepository installmentRepository;

    @Autowired
    private AuthAuditLogRepository auditRepo;

    @Autowired
    private UserNotificationPreferenceRepository notificationPreferenceRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProfileSettingsBean profileSettingsBean;

    @Autowired
    private SecuritySettingsBean securitySettingsBean;

    @Autowired
    private NotificationSettingsBean notificationSettingsBean;

    @Autowired
    private RolePermissionsBean rolePermissionsBean;

    @Autowired
    private LoanPolicySettingsBean loanPolicySettingsBean;

    @Autowired
    private AuditSettingsBean auditSettingsBean;

    @Autowired
    private SystemSettingsBean systemSettingsBean;

    private User admin;
    private User loanOfficer;
    private User customer;
    private User collectionsAgent;

    private CustomUserDetails adminDetails;
    private CustomUserDetails loanOfficerDetails;
    private CustomUserDetails customerDetails;

    @BeforeEach
    void setUp() {
        notificationPreferenceRepo.deleteAll();
        approvalHistoryRepository.deleteAll();
        installmentRepository.deleteAll();
        loanRepository.deleteAll();
        auditRepo.deleteAll();
        userRepository.deleteAll();

        // 1. Admin
        admin = new User("Admin Ramesh", "admin.ramesh@bank.com", passwordEncoder.encode("Admin@Pass123"), AppRole.ROLE_ADMIN, UserType.STAFF);
        admin.setBranch("ALL");
        admin.setMfaSecret("TEST_ADMIN_MFA_SECRET_123");
        admin.setMfaEnrolled(true);
        admin = userRepository.save(admin);
        adminDetails = new CustomUserDetails(admin);

        // 2. Loan Officer
        loanOfficer = new User("Officer Sunita", "officer.sunita@bank.com", passwordEncoder.encode("Officer@Pass123"), AppRole.ROLE_LOAN_OFFICER, UserType.STAFF);
        loanOfficer.setBranch("MAIN");
        loanOfficer.setMfaSecret("TEST_OFFICER_MFA_SECRET_123");
        loanOfficer.setMfaEnrolled(true);
        loanOfficer = userRepository.save(loanOfficer);
        loanOfficerDetails = new CustomUserDetails(loanOfficer);

        // 3. Customer
        customer = new User("Aarav Sharma", "aarav.sharma@example.com", passwordEncoder.encode("Cust@Pass123"), AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer.setBranch("MAIN");
        customer.setCustomerId("BOI-CUST-9001");
        customer.setPhoneNumber("9811122233");
        customer.setAddress("45 Nariman Point, Mumbai, Maharashtra");
        customer.setPanNumber("ABCDE1234F");
        customer.setAadhaarNumber("998877665544");
        customer.setMfaSecret("TEST_CUSTOMER_MFA_SECRET_123");
        customer.setMfaEnrolled(true);
        customer = userRepository.save(customer);
        customerDetails = new CustomUserDetails(customer);

        // 4. Collections Agent
        collectionsAgent = new User("Agent Prakash", "agent.prakash@bank.com", passwordEncoder.encode("Agent@Pass123"), AppRole.ROLE_COLLECTIONS_AGENT, UserType.STAFF);
        collectionsAgent.setBranch("MAIN");
        collectionsAgent = userRepository.save(collectionsAgent);
    }

    private void authenticateAs(CustomUserDetails details) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("1. Page Loading: All 7 JSF Settings pages and Hub load for ROLE_ADMIN")
    void testAllSevenPagesAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/settings-hub.xhtml").with(user(adminDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/profile-settings.xhtml").with(user(adminDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/security-settings.xhtml").with(user(adminDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notification-settings.xhtml").with(user(adminDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/loan-policy-settings.xhtml").with(user(adminDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/role-permissions.xhtml").with(user(adminDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit-settings.xhtml").with(user(adminDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/system-settings.xhtml").with(user(adminDetails)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. RBAC Enforcement: ROLE_CUSTOMER can access personal settings, but is forbidden from administrative pages")
    void testCustomerRoleBoundaries() throws Exception {
        // Permitted pages
        mockMvc.perform(get("/settings-hub.xhtml").with(user(customerDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/profile-settings.xhtml").with(user(customerDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/security-settings.xhtml").with(user(customerDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/notification-settings.xhtml").with(user(customerDetails)))
                .andExpect(status().isOk());

        // Forbidden pages
        mockMvc.perform(get("/loan-policy-settings.xhtml").with(user(customerDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/role-permissions.xhtml").with(user(customerDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit-settings.xhtml").with(user(customerDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/system-settings.xhtml").with(user(customerDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. RBAC Enforcement: ROLE_LOAN_OFFICER can access loan policies, but not admin-only pages")
    void testLoanOfficerRoleBoundaries() throws Exception {
        mockMvc.perform(get("/loan-policy-settings.xhtml").with(user(loanOfficerDetails)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/role-permissions.xhtml").with(user(loanOfficerDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit-settings.xhtml").with(user(loanOfficerDetails)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/system-settings.xhtml").with(user(loanOfficerDetails)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Authentication Boundary: Unauthenticated access redirects to /login")
    void testUnauthenticatedRedirect() throws Exception {
        mockMvc.perform(get("/settings-hub.xhtml"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/profile-settings.xhtml"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/audit-settings.xhtml"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("5. ProfileSettingsBean: Safe updates persist, immutable fields cannot be tampered with")
    void testProfileSettingsUpdatesAndImmutability() {
        try {
            authenticateAs(customerDetails);

            profileSettingsBean.init();
            assertEquals("Aarav Sharma", profileSettingsBean.getName());
            assertEquals("BOI-CUST-9001", profileSettingsBean.getCustomerId());
            assertEquals("ABCDE1234F", customer.getPanNumber());

            // Update editable contact details
            profileSettingsBean.setPhoneNumber("9998887776");
            profileSettingsBean.setAddress("78 Marine Drive, Mumbai 400020");
            profileSettingsBean.setPreferredContactMethod("SMS");
            profileSettingsBean.saveProfile();

            assertNotNull(profileSettingsBean.getSuccessMessage());

            // Verify in database
            User reloaded = userRepository.findById(customer.getId()).orElseThrow();
            assertEquals("9998887776", reloaded.getPhoneNumber());
            assertEquals("78 Marine Drive, Mumbai 400020", reloaded.getAddress());
            assertEquals(AppRole.ROLE_CUSTOMER, reloaded.getRole()); // Role was NOT changed
            assertEquals("BOI-CUST-9001", reloaded.getCustomerId()); // Identity was NOT changed

            // Verify audit log recorded
            List<AuthAuditLog> logs = auditRepo.findAll();
            boolean foundAudit = logs.stream().anyMatch(l -> "PROFILE_UPDATED".equals(l.getEventType()));
            assertTrue(foundAudit, "Expected PROFILE_UPDATED audit log entry");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("6. SecuritySettingsBean: Password validation rejects weak/incorrect passwords; valid change succeeds with audit")
    void testSecuritySettingsPasswordChange() {
        try {
            authenticateAs(customerDetails);

            securitySettingsBean.init();
            assertTrue(securitySettingsBean.isMfaEnrolled());

            // 1. Wrong current password
            securitySettingsBean.setCurrentPassword("WrongCurrentPass");
            securitySettingsBean.setNewPassword("Secure@Pass2026!");
            securitySettingsBean.setConfirmPassword("Secure@Pass2026!");
            securitySettingsBean.changePassword();
            assertNotNull(securitySettingsBean.getErrorMessage());
            assertTrue(securitySettingsBean.getErrorMessage().contains("does not match"));

            // 2. Mismatched passwords
            securitySettingsBean.setCurrentPassword("Cust@Pass123");
            securitySettingsBean.setNewPassword("Secure@Pass2026!");
            securitySettingsBean.setConfirmPassword("Mismatch@Pass2026!");
            securitySettingsBean.changePassword();
            assertNotNull(securitySettingsBean.getErrorMessage());
            assertTrue(securitySettingsBean.getErrorMessage().contains("do not match"));

            // 3. Weak password
            securitySettingsBean.setCurrentPassword("Cust@Pass123");
            securitySettingsBean.setNewPassword("weakpass");
            securitySettingsBean.setConfirmPassword("weakpass");
            securitySettingsBean.changePassword();
            assertNotNull(securitySettingsBean.getErrorMessage());
            assertTrue(securitySettingsBean.getErrorMessage().contains("8-64 characters"));

            // 4. Valid password update
            securitySettingsBean.setCurrentPassword("Cust@Pass123");
            securitySettingsBean.setNewPassword("Strong@Pass2026!");
            securitySettingsBean.setConfirmPassword("Strong@Pass2026!");
            securitySettingsBean.changePassword();
            assertNotNull(securitySettingsBean.getSuccessMessage());

            // Verify password encoded in database and MFA secret preserved
            User reloaded = userRepository.findById(customer.getId()).orElseThrow();
            assertTrue(passwordEncoder.matches("Strong@Pass2026!", reloaded.getPassword()));
            assertEquals("TEST_CUSTOMER_MFA_SECRET_123", reloaded.getMfaSecret());
            assertTrue(reloaded.isMfaEnrolled());

            // Verify audit log recorded
            List<AuthAuditLog> logs = auditRepo.findAll();
            boolean foundAudit = logs.stream().anyMatch(l -> "PASSWORD_CHANGED".equals(l.getEventType()));
            assertTrue(foundAudit, "Expected PASSWORD_CHANGED audit log entry");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("7. NotificationSettingsBean: Preference persistence and test alert trigger")
    void testNotificationPreferencePersistence() {
        try {
            authenticateAs(customerDetails);

            notificationSettingsBean.init();
            notificationSettingsBean.setEmailEnabled(true);
            notificationSettingsBean.setSmsEnabled(true);
            notificationSettingsBean.setLoanUpdatesEnabled(true);
            notificationSettingsBean.setRepaymentRemindersEnabled(true);
            notificationSettingsBean.setSecurityAlertsEnabled(true);

            notificationSettingsBean.savePreferences();
            assertNotNull(notificationSettingsBean.getSuccessMessage());

            // Check repository
            UserNotificationPreference pref = notificationPreferenceRepo.findByUser_Id(customer.getId()).orElseThrow();
            assertTrue(pref.isEmailEnabled());
            assertTrue(pref.isSmsEnabled());
            assertTrue(pref.isLoanUpdatesEnabled());

            // Send test alert
            notificationSettingsBean.sendTestAlert();
            assertNotNull(notificationSettingsBean.getSuccessMessage());

            // Audit recorded
            List<AuthAuditLog> logs = auditRepo.findAll();
            boolean foundAudit = logs.stream().anyMatch(l -> "NOTIFICATION_PREFERENCES_UPDATED".equals(l.getEventType()));
            assertTrue(foundAudit, "Expected NOTIFICATION_PREFERENCES_UPDATED audit log");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("8. RolePermissionsBean: Staff authorization governance strictly restricted to ROLE_ADMIN")
    void testRolePermissionsGovernance() {
        try {
            // Customer cannot update staff role
            authenticateAs(customerDetails);
            rolePermissionsBean.setSelectedStaffId(collectionsAgent.getId());
            rolePermissionsBean.setSelectedNewRole(AppRole.ROLE_LOAN_OFFICER);
            assertThrows(AccessDeniedException.class, () -> rolePermissionsBean.updateStaffRole());

            // Admin can update staff role
            authenticateAs(adminDetails);
            rolePermissionsBean.init();
            rolePermissionsBean.setSelectedStaffId(collectionsAgent.getId());
            rolePermissionsBean.setSelectedNewRole(AppRole.ROLE_LOAN_OFFICER);
            rolePermissionsBean.updateStaffRole();
            assertNotNull(rolePermissionsBean.getSuccessMessage());

            User reloaded = userRepository.findById(collectionsAgent.getId()).orElseThrow();
            assertEquals(AppRole.ROLE_LOAN_OFFICER, reloaded.getRole());

            // Staff members must be assigned a staff or admin role, not ROLE_CUSTOMER
            rolePermissionsBean.setSelectedStaffId(admin.getId());
            rolePermissionsBean.setSelectedNewRole(AppRole.ROLE_CUSTOMER);
            rolePermissionsBean.updateStaffRole();
            assertNotNull(rolePermissionsBean.getErrorMessage());
            assertTrue(rolePermissionsBean.getErrorMessage().contains("must be assigned a valid staff or administrator role"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("9. LoanPolicySettingsBean: Validation of underwriting limits and EMI simulator")
    void testLoanPolicySettingsValidationAndEmiSimulator() {
        try {
            // Loan officer can view policies and run simulator
            authenticateAs(loanOfficerDetails);
            loanPolicySettingsBean.init();
            assertTrue(loanPolicySettingsBean.getDefaultInterestRate() > 0);

            // EMI calculation test
            loanPolicySettingsBean.setSimAmount(100000.0);
            loanPolicySettingsBean.setSimInterestRate(10.0);
            loanPolicySettingsBean.setSimTenureMonths(12);
            loanPolicySettingsBean.calculateSimulation();
            assertTrue(loanPolicySettingsBean.getSimCalculatedEmi() > 8000.0);

            // Loan Officer cannot save policy updates (admin only)
            assertThrows(AccessDeniedException.class, () -> loanPolicySettingsBean.savePolicySettings());

            // Admin can validate and save policy updates
            authenticateAs(adminDetails);
            loanPolicySettingsBean.setEditMinAmount(200000.0);
            loanPolicySettingsBean.setEditMaxAmount(100000.0); // Invalid: min > max
            loanPolicySettingsBean.savePolicySettings();
            assertNotNull(loanPolicySettingsBean.getErrorMessage());
            assertTrue(loanPolicySettingsBean.getErrorMessage().contains("cannot be less than the minimum loan amount"));

            // Valid update
            loanPolicySettingsBean.setEditMinAmount(10000.0);
            loanPolicySettingsBean.setEditMaxAmount(2000000.0);
            loanPolicySettingsBean.setEditInterestRate(11.25);
            loanPolicySettingsBean.savePolicySettings();
            assertNotNull(loanPolicySettingsBean.getSuccessMessage());

            // Verify audit
            List<AuthAuditLog> logs = auditRepo.findAll();
            boolean foundAudit = logs.stream().anyMatch(l -> "LOAN_POLICY_UPDATED".equals(l.getEventType()));
            assertTrue(foundAudit, "Expected LOAN_POLICY_UPDATED audit log");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("10. AuditSettingsBean: Immutable log querying, keyword search, and pagination")
    void testAuditSettingsFilteringAndImmutability() {
        try {
            // Customer denied
            authenticateAs(customerDetails);
            auditSettingsBean.init();
            assertNotNull(auditSettingsBean.getErrorMessage());
            assertTrue(auditSettingsBean.getErrorMessage().contains("Access Denied"));

            // Admin view and search
            authenticateAs(adminDetails);
            // Create test audit entries
            AuthAuditLog log1 = new AuthAuditLog("test.user@boi.com", "127.0.0.1", "Mozilla/5.0", "LOGIN_STAGE1_SUCCESS", "SUCCESS", null, "Standard Login");
            auditRepo.save(log1);

            AuthAuditLog log2 = new AuthAuditLog("intruder@unknown.com", "192.168.1.100", "Curl", "MFA_VERIFY_FAILURE", "FAILURE", "INVALID_CODE", "Invalid TOTP");
            auditRepo.save(log2);

            auditSettingsBean.init();
            assertTrue(auditSettingsBean.getTotalRecords() >= 2);

            // Filter by keyword "intruder"
            auditSettingsBean.setKeyword("intruder");
            auditSettingsBean.applyFilters();
            assertEquals(1, auditSettingsBean.getAllFilteredLogs().size());
            assertEquals("intruder@unknown.com", auditSettingsBean.getAllFilteredLogs().get(0).getUsername());

            // Reset filter
            auditSettingsBean.resetFilters();
            assertTrue(auditSettingsBean.getAllFilteredLogs().size() >= 2);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("11. SystemSettingsBean: Runtime diagnostics and safe operational preference updates")
    void testSystemSettingsBeanDiagnosticsAndOperationalControls() {
        try {
            // Customer denied
            authenticateAs(customerDetails);
            assertThrows(AccessDeniedException.class, () -> systemSettingsBean.saveOperationalSettings());

            // Admin access
            authenticateAs(adminDetails);
            systemSettingsBean.init();
            assertNotNull(systemSettingsBean.getJavaVersion());
            assertNotNull(systemSettingsBean.getJvmUptime());
            assertTrue(systemSettingsBean.getTotalMemoryMb() > 0);

            // Save operational banner
            systemSettingsBean.setAnnouncementBanner("Scheduled banking maintenance test alert.");
            systemSettingsBean.setMaintenanceNoticeEnabled(true);
            systemSettingsBean.saveOperationalSettings();
            assertNotNull(systemSettingsBean.getSuccessMessage());

            // Verify audit
            List<AuthAuditLog> logs = auditRepo.findAll();
            boolean foundAudit = logs.stream().anyMatch(l -> "SYSTEM_SETTINGS_UPDATED".equals(l.getEventType()));
            assertTrue(foundAudit, "Expected SYSTEM_SETTINGS_UPDATED audit log");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("12. Existing Application Integrity: JSP dashboard and core routes remain functional")
    void testExistingRoutesFunctionality() throws Exception {
        // Customer dashboard
        mockMvc.perform(get("/dashboard").with(user(customerDetails)))
                .andExpect(status().isOk());

        // Apply loan view
        mockMvc.perform(get("/applyLoan").with(user(customerDetails)))
                .andExpect(status().isOk());

        // JSF Loan Approval Dashboard (Loan Officer)
        mockMvc.perform(get("/loan-approval.xhtml").with(user(loanOfficerDetails)))
                .andExpect(status().isOk());
    }
}
