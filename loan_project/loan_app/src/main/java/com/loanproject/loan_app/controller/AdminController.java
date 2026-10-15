package com.loanproject.loan_app.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.security.AuthAuditService;
import com.loanproject.loan_app.security.LoginAttemptService;
import com.loanproject.loan_app.services.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final AuthAuditLogRepository auditRepo;
    private final AuthAuditService auditService;

    public AdminController(UserService userService,
                           LoginAttemptService loginAttemptService,
                           AuthAuditLogRepository auditRepo,
                           AuthAuditService auditService) {
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
        this.auditRepo = auditRepo;
        this.auditService = auditService;
    }

    @GetMapping("/staff")
    public String listStaff(ModelMap model) {
        List<User> staffList = userService.findAllStaff();
        model.addAttribute("staffList", staffList);
        model.addAttribute("roles", List.of(
                AppRole.ROLE_LOAN_OFFICER,
                AppRole.ROLE_COLLECTIONS_AGENT,
                AppRole.ROLE_COLLECTIONS_MANAGER,
                AppRole.ROLE_ADMIN
        ));
        return "admin-staff";
    }

    @PostMapping("/staff/provision")
    public String provisionStaff(@RequestParam("name") String name,
                                 @RequestParam("email") String email,
                                 @RequestParam("password") String password,
                                 @RequestParam("role") AppRole role,
                                 HttpServletRequest request,
                                 Authentication authentication,
                                 ModelMap model) {
        try {
            userService.provisionStaff(name, email, password, role, "TOTP");
            auditService.logStaffProvisioned(authentication.getName(), request, email, role.name());
            return "redirect:/admin/staff?success=provisioned";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return listStaff(model);
        }
    }

    @PostMapping("/staff/unlock")
    public String unlockStaff(@RequestParam("email") String email,
                              Authentication authentication,
                              HttpServletRequest request) {
        loginAttemptService.unlockAccount(email, authentication.getName(), request);
        return "redirect:/admin/staff?success=unlocked";
    }

    @PostMapping("/staff/role")
    public String updateRole(@RequestParam("staffId") Long staffId,
                             @RequestParam("newRole") AppRole newRole,
                             Authentication authentication,
                             HttpServletRequest request) {
        userService.findById(staffId).ifPresent(staff -> {
            userService.updateStaffRole(staffId, newRole);
            auditService.logRoleChanged(authentication.getName(), request, staff.getEmail(), newRole.name());
        });
        return "redirect:/admin/staff?success=role_updated";
    }

    @PostMapping("/staff/reset-mfa")
    public String resetMfa(@RequestParam("staffId") Long staffId) {
        userService.resetMfaSecret(staffId);
        return "redirect:/admin/staff?success=mfa_reset";
    }

    @GetMapping("/audit-logs")
    public String viewAuditLogs(ModelMap model) {
        List<AuthAuditLog> logs = auditRepo.findTop100ByOrderByTimestampDesc();
        model.addAttribute("auditLogs", logs);
        return "admin-audits";
    }
}
