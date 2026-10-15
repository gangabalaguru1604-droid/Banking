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
import com.loanproject.loan_app.bean.UserType;
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.loanproject.loan_app.services.DpdReminderScheduler reminderScheduler;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.loanproject.loan_app.services.DpdReminderService reminderService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.loanproject.loan_app.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.loanproject.loan_app.repository.LoanRepository loanRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.loanproject.loan_app.repository.RepaymentInstallmentRepository installmentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.loanproject.loan_app.repository.LoanReminderLogRepository reminderLogRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.loanproject.loan_app.services.DefaultEmailReminderProvider emailReminderProvider;

    @GetMapping("/reminders/demo")
    public String viewRemindersDemo(
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "error", required = false) String error,
            ModelMap model) {

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate minDue = today.minusDays(3);
        java.time.LocalDate maxDue = today.minusDays(1);

        if (installmentRepository != null) {
            List<com.loanproject.loan_app.bean.RepaymentInstallment> overdueList =
                    installmentRepository.findEligibleOverdueInstallmentsList(minDue, maxDue);
            model.addAttribute("overdueInstallments", overdueList);
        }

        if (reminderLogRepository != null) {
            List<com.loanproject.loan_app.bean.LoanReminderLog> logs =
                    reminderLogRepository.findAll(org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
            model.addAttribute("reminderLogs", logs);
        }

        model.addAttribute("today", today);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "admin-reminders";
    }

    @PostMapping("/reminders/demo/create-overdue-loan")
    public String createDemoOverdueLoan(
            @RequestParam("customerName") String customerName,
            @RequestParam("customerEmail") String customerEmail,
            @RequestParam(value = "customerPhone", defaultValue = "9876543210") String customerPhone,
            @RequestParam(value = "emiAmount", defaultValue = "4500.00") java.math.BigDecimal emiAmount,
            @RequestParam(value = "dpdDays", defaultValue = "1") int dpdDays,
            @RequestParam(value = "sendImmediately", defaultValue = "true") boolean sendImmediately) {

        if (userRepository == null || loanRepository == null || installmentRepository == null) {
            return "redirect:/admin/reminders/demo?error=services_unavailable";
        }

        String emailClean = customerEmail.trim().toLowerCase();
        User customer = userRepository.findByEmail(emailClean).orElseGet(() -> {
            User u = new User();
            u.setName(customerName.trim());
            u.setEmail(emailClean);
            u.setPassword("Password123!"); // Default temporary password
            u.setRole(AppRole.ROLE_CUSTOMER);
            u.setUserType(UserType.CUSTOMER);
            u.setPhoneNumber(customerPhone);
            u.setBranch("MAIN");
            u.setCustomerId("CUST" + System.currentTimeMillis() % 1000000);
            return userRepository.save(u);
        });

        com.loanproject.loan_app.bean.Loan loan = new com.loanproject.loan_app.bean.Loan();
        loan.setUser(customer);
        loan.setAmount(emiAmount.doubleValue() * 12);
        loan.setInterest(12.0);
        loan.setMonths(12);
        loan.setEmi(emiAmount.doubleValue());
        loan.setRemaining(emiAmount.doubleValue() * 12);
        loan.setBranch("MAIN");
        loan.setStatus("ACTIVE");
        loan.setProductType("Personal Loan");
        loan.setInterestType("FIXED");
        loan.setDisbursedAmount(emiAmount.doubleValue() * 12);
        loan.setDisbursementDate(java.time.LocalDate.now().minusMonths(1));
        loan.setAccountNumber("LN" + System.currentTimeMillis());
        com.loanproject.loan_app.bean.Loan savedLoan = loanRepository.save(loan);

        java.time.LocalDate dueDate = java.time.LocalDate.now().minusDays(dpdDays);
        com.loanproject.loan_app.bean.RepaymentInstallment installment = new com.loanproject.loan_app.bean.RepaymentInstallment(
                savedLoan,
                1,
                dueDate,
                emiAmount,
                emiAmount.multiply(new java.math.BigDecimal("0.8")).setScale(2, java.math.RoundingMode.HALF_UP),
                emiAmount.multiply(new java.math.BigDecimal("0.2")).setScale(2, java.math.RoundingMode.HALF_UP),
                java.math.BigDecimal.ZERO,
                "DUE",
                dpdDays
        );
        installmentRepository.save(installment);

        if (sendImmediately && reminderScheduler != null) {
            reminderScheduler.runDailyReminderJob(java.time.LocalDate.now());
            return "redirect:/admin/reminders/demo?success=loan_created_and_sent";
        }

        return "redirect:/admin/reminders/demo?success=loan_created";
    }

    @PostMapping("/reminders/demo/trigger-now")
    public String triggerReminderNow() {
        if (reminderScheduler != null) {
            reminderScheduler.runDailyReminderJob(java.time.LocalDate.now());
        }
        return "redirect:/admin/reminders/demo?success=scheduler_executed";
    }

    @GetMapping(value = "/reminders/demo/preview-html", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    @org.springframework.web.bind.annotation.ResponseBody
    public String previewEmailHtml(
            @RequestParam("installmentId") Long installmentId,
            @RequestParam(value = "dpd", defaultValue = "1") int dpd) {

        if (installmentRepository == null || emailReminderProvider == null) {
            return "<h3>Service unavailable</h3>";
        }

        com.loanproject.loan_app.bean.RepaymentInstallment inst = installmentRepository.findById(installmentId).orElse(null);
        if (inst == null) {
            return "<h3>Installment not found: " + installmentId + "</h3>";
        }

        User u = inst.getLoan().getUser();
        String customerName = u != null ? u.getName() : "Customer";
        String email = u != null ? u.getEmail() : "customer@example.com";
        String phone = u != null ? u.getPhoneNumber() : "9876543210";
        String masked = com.loanproject.loan_app.security.DataMaskingUtils.maskAccountNumber(inst.getLoan().getAccountNumber());
        java.math.BigDecimal penalty = (reminderService != null) ? reminderService.getPenaltyForDpd(dpd) : java.math.BigDecimal.ZERO;
        String payUrl = "http://localhost:8081/payPage?account=" + inst.getLoan().getAccountNumber();

        com.loanproject.loan_app.dto.ReminderMessageRequest req = new com.loanproject.loan_app.dto.ReminderMessageRequest(
                inst.getLoan().getLoanId(),
                inst.getId(),
                inst.getInstallmentNumber(),
                customerName,
                email,
                phone,
                masked,
                inst.getEmiAmount().subtract(inst.getPaidAmount()),
                inst.getDueDate(),
                dpd,
                penalty,
                payUrl,
                "1800-200-8899",
                inst.getDueDate().toString()
        );

        return emailReminderProvider.previewHtml(req);
    }

    @PostMapping("/reminders/demo/simulate-webhook")
    public String simulateWebhook(
            @RequestParam("providerMessageId") String providerMessageId,
            @RequestParam("eventType") String eventType) {

        if (reminderService != null) {
            reminderService.processProviderWebhook(providerMessageId, eventType, "Simulated via Admin Demo Interface");
        }
        return "redirect:/admin/reminders/demo?success=webhook_" + eventType.toLowerCase();
    }

    @PostMapping("/reminders/trigger")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> triggerReminderJob(
            @RequestParam(value = "date", required = false) String dateStr) {
        java.time.LocalDate businessDate = (dateStr != null && !dateStr.isBlank())
                ? java.time.LocalDate.parse(dateStr.trim())
                : java.time.LocalDate.now();
        int processed = (reminderScheduler != null) ? reminderScheduler.runDailyReminderJob(businessDate) : 0;
        return java.util.Map.of(
                "status", "SUCCESS",
                "businessDate", businessDate.toString(),
                "processedInstallments", processed
        );
    }
}
