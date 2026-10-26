package com.loanproject.loan_app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.dto.ActiveBorrowerDto;
import com.loanproject.loan_app.dto.StaffDashboardLoanDto;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanService;
import com.loanproject.loan_app.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoanController {

    @Autowired
    private LoanService service;

    @Autowired
    private UserService userService;

    @Autowired
    private com.loanproject.loan_app.repository.PaymentTransactionRepository paymentTransactionRepository;

    @Autowired(required = false)
    private com.loanproject.loan_app.security.AuthAuditService auditService;

    @Autowired(required = false)
    private com.loanproject.loan_app.repository.RepaymentInstallmentRepository installmentRepository;

    private User getAuthenticatedUser(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }
        User u = (User) session.getAttribute("user");
        if (u == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        return u;
    }

    @GetMapping("/dashboard")
    public String dash(HttpSession s, ModelMap m) {
        User u = getAuthenticatedUser(s);
        m.addAttribute("currentUser", u);

        if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            java.util.List<Loan> customerLoans = service.get(u);
            m.addAttribute("loans", customerLoans);
            m.addAttribute("isCustomer", true);

            // Compute portfolio totals across all borrower loans
            double totalPrincipal = customerLoans.stream().mapToDouble(Loan::getAmount).sum();
            double totalRemaining = customerLoans.stream().mapToDouble(Loan::getRemaining).sum();
            double totalEmi = customerLoans.stream().mapToDouble(Loan::getEmi).sum();
            
            int totalLoanCount = customerLoans.size();
            int activeLoanCount = (int) customerLoans.stream()
                    .filter(l -> !"CLOSED".equalsIgnoreCase(l.getStatus())
                              && !"REJECTED".equalsIgnoreCase(l.getStatus())
                              && !"PENDING".equalsIgnoreCase(l.getStatus())
                              && !"SUBMITTED".equalsIgnoreCase(l.getStatus())
                              && !"IN_REVIEW".equalsIgnoreCase(l.getStatus()))
                    .count();
            int pendingLoanCount = (int) customerLoans.stream()
                    .filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())
                              || "SUBMITTED".equalsIgnoreCase(l.getStatus())
                              || "IN_REVIEW".equalsIgnoreCase(l.getStatus())
                              || "APPLIED".equalsIgnoreCase(l.getStatus()))
                    .count();

            // Explicit BigDecimal for accurate monetary display
            java.math.BigDecimal outstandingBalance = customerLoans.stream()
                    .map(l -> java.math.BigDecimal.valueOf(l.getRemaining()))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            m.addAttribute("totalPrincipal", totalPrincipal);
            m.addAttribute("totalRemaining", totalRemaining);
            m.addAttribute("outstandingBalance", outstandingBalance);
            m.addAttribute("totalEmi", totalEmi);
            m.addAttribute("activeLoanCount", activeLoanCount);
            m.addAttribute("pendingLoanCount", pendingLoanCount);
            m.addAttribute("totalLoanCount", totalLoanCount);

            // Fetch persisted recent activities
            java.util.List<com.loanproject.loan_app.dto.CustomerActivityDto> activities = getCustomerRecentActivities(customerLoans);
            m.addAttribute("recentActivities", activities);

            // Provide single loan attribute if exactly one loan exists for backward compatibility
            if (customerLoans.size() == 1) {
                m.addAttribute("loan", customerLoans.get(0));
            }
        } else {
            // Staff view: filter loans by staff's branch (or ALL for admin) and mask customer emails by default
            String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(u.getBranch()))
                    ? "ALL"
                    : (u.getBranch() != null ? u.getBranch() : "MAIN");

            List<Loan> rawBranchLoans = service.getAll().stream()
                    .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                    .toList();

            java.util.Map<Long, java.util.List<com.loanproject.loan_app.bean.RepaymentInstallment>> preloadedInstallments =
                    preloadInstallmentsByLoanId(rawBranchLoans);

            List<com.loanproject.loan_app.dto.StaffDashboardLoanDto> staffLoans = rawBranchLoans.stream()
                    .map(l -> mapToStaffLoanDto(l, preloadedInstallments))
                    .toList();

            // Portfolio Aggregates
            double totalPrincipal = staffLoans.stream().mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getAmount).sum();
            double totalRemaining = staffLoans.stream().mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getRemaining).sum();
            double totalOverdueAmount = staffLoans.stream().mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getOverdueAmount).sum();

            int totalLoansCount = staffLoans.size();
            int activeLoansCount = (int) staffLoans.stream().filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus())).count();
            int closedLoansCount = (int) staffLoans.stream().filter(l -> "CLOSED".equalsIgnoreCase(l.getStatus())).count();
            int pendingLoansCount = (int) staffLoans.stream().filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())
                    || "SUBMITTED".equalsIgnoreCase(l.getStatus())
                    || "IN_REVIEW".equalsIgnoreCase(l.getStatus())
                    || "APPLIED".equalsIgnoreCase(l.getStatus())).count();
            int overdueLoansCount = (int) staffLoans.stream().filter(l -> "OVERDUE".equalsIgnoreCase(l.getRepaymentStatus()) || l.getDpd() > 0).count();

            // DPD bucket aggregations (for Collections Agent and Collections Manager)
            long dpdCurrentCount = staffLoans.stream().filter(l -> l.getDpd() == 0 && !"CLOSED".equalsIgnoreCase(l.getStatus()) && !"REJECTED".equalsIgnoreCase(l.getStatus())).count();
            double dpdCurrentAmount = staffLoans.stream().filter(l -> l.getDpd() == 0 && !"CLOSED".equalsIgnoreCase(l.getStatus()) && !"REJECTED".equalsIgnoreCase(l.getStatus())).mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getRemaining).sum();

            long dpdSma0Count = staffLoans.stream().filter(l -> l.getDpd() >= 1 && l.getDpd() <= 30).count();
            double dpdSma0Amount = staffLoans.stream().filter(l -> l.getDpd() >= 1 && l.getDpd() <= 30).mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getOverdueAmount).sum();

            long dpdSma1Count = staffLoans.stream().filter(l -> l.getDpd() >= 31 && l.getDpd() <= 60).count();
            double dpdSma1Amount = staffLoans.stream().filter(l -> l.getDpd() >= 31 && l.getDpd() <= 60).mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getOverdueAmount).sum();

            long dpdSma2Count = staffLoans.stream().filter(l -> l.getDpd() >= 61 && l.getDpd() <= 90).count();
            double dpdSma2Amount = staffLoans.stream().filter(l -> l.getDpd() >= 61 && l.getDpd() <= 90).mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getOverdueAmount).sum();

            long dpdNpaCount = staffLoans.stream().filter(l -> l.getDpd() > 90).count();
            double dpdNpaAmount = staffLoans.stream().filter(l -> l.getDpd() > 90).mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getOverdueAmount).sum();

            double parRate = totalRemaining > 0 ? (totalOverdueAmount / totalRemaining) * 100.0 : 0.0;

            // Role-specific lists
            List<com.loanproject.loan_app.dto.StaffDashboardLoanDto> pendingLoans = staffLoans.stream()
                    .filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())
                            || "SUBMITTED".equalsIgnoreCase(l.getStatus())
                            || "IN_REVIEW".equalsIgnoreCase(l.getStatus())
                            || "APPLIED".equalsIgnoreCase(l.getStatus()))
                    .toList();

            List<com.loanproject.loan_app.dto.StaffDashboardLoanDto> delinquentLoans = staffLoans.stream()
                    .filter(l -> ("OVERDUE".equalsIgnoreCase(l.getRepaymentStatus()) || l.getDpd() > 0) && !"CLOSED".equalsIgnoreCase(l.getStatus()))
                    .toList();

            List<com.loanproject.loan_app.dto.StaffDashboardLoanDto> activeLoans = staffLoans.stream()
                    .filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()))
                    .toList();

            // Recent branch collections
            List<com.loanproject.loan_app.bean.PaymentTransaction> recentCollections = java.util.List.of();
            if (!rawBranchLoans.isEmpty()) {
                try {
                    recentCollections = paymentTransactionRepository.findByLoanInOrderByPaymentDateDesc(rawBranchLoans).stream()
                            .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                            .limit(10)
                            .toList();
                } catch (Exception ignored) {}
            }

            List<com.loanproject.loan_app.dto.ActiveBorrowerDto> activeBorrowers = buildActiveBorrowers(rawBranchLoans, u);
            int activeBorrowersCount = activeBorrowers.size();

            List<String> allBranches = staffLoans.stream()
                    .map(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getBranch)
                    .filter(b -> b != null && !b.isBlank())
                    .distinct()
                    .sorted()
                    .toList();

            List<String> allStatuses = staffLoans.stream()
                    .map(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getStatus)
                    .filter(st -> st != null && !st.isBlank())
                    .distinct()
                    .sorted()
                    .toList();

            m.addAttribute("loans", staffLoans);
            m.addAttribute("activeBorrowers", activeBorrowers);
            m.addAttribute("allBranches", allBranches);
            m.addAttribute("allStatuses", allStatuses);
            m.addAttribute("pendingLoans", pendingLoans);
            m.addAttribute("delinquentLoans", delinquentLoans);
            m.addAttribute("activeLoans", activeLoans);
            m.addAttribute("recentCollections", recentCollections);

            m.addAttribute("totalPrincipal", totalPrincipal);
            m.addAttribute("totalRemaining", totalRemaining);
            m.addAttribute("totalOverdueAmount", totalOverdueAmount);
            m.addAttribute("totalLoansCount", totalLoansCount);
            m.addAttribute("activeLoansCount", activeLoansCount);
            m.addAttribute("activeBorrowersCount", activeBorrowersCount);
            m.addAttribute("closedLoansCount", closedLoansCount);
            m.addAttribute("pendingLoansCount", pendingLoansCount);
            m.addAttribute("overdueLoansCount", overdueLoansCount);

            m.addAttribute("dpdCurrentCount", dpdCurrentCount);
            m.addAttribute("dpdCurrentAmount", dpdCurrentAmount);
            m.addAttribute("dpdSma0Count", dpdSma0Count);
            m.addAttribute("dpdSma0Amount", dpdSma0Amount);
            m.addAttribute("dpdSma1Count", dpdSma1Count);
            m.addAttribute("dpdSma1Amount", dpdSma1Amount);
            m.addAttribute("dpdSma2Count", dpdSma2Count);
            m.addAttribute("dpdSma2Amount", dpdSma2Amount);
            m.addAttribute("dpdNpaCount", dpdNpaCount);
            m.addAttribute("dpdNpaAmount", dpdNpaAmount);
            m.addAttribute("parRate", Math.round(parRate * 100.0) / 100.0);

            m.addAttribute("isStaff", true);
            m.addAttribute("userRole", u.getRole().name());
            m.addAttribute("officerBranch", branchFilter);
        }

        return "dashboard";
    }

    @GetMapping("/applyLoan")
    public String form(HttpSession s, ModelMap m) {
        User u = getAuthenticatedUser(s);

        // RBAC: Collections agents and managers cannot originate loans
        if (u.getRole() == AppRole.ROLE_COLLECTIONS_AGENT || u.getRole() == AppRole.ROLE_COLLECTIONS_MANAGER) {
            throw new AccessDeniedException("Collections staff are not authorized to originate loans.");
        }

        m.addAttribute("currentUser", u);
        m.addAttribute("loanSlabs", service.getLoanSlabs());
        m.addAttribute("defaultInterestRate", service.getDefaultInterestRate());
        m.addAttribute("defaultTenureMonths", service.getDefaultTenureMonths());
        m.addAttribute("minLoanAmount", service.getMinAmount());
        m.addAttribute("maxLoanAmount", service.getMaxAmount());
        return "applyLoan";
    }

    @PostMapping("/applyLoan")
    public String apply(HttpSession s,
                        @RequestParam(value = "amount") double amount,
                        @RequestParam(value = "interest", required = false) Double clientInterest,
                        @RequestParam(value = "months", required = false) Integer clientMonths,
                        @RequestParam(value = "customerEmail", required = false) String customerEmail) {
        User u = getAuthenticatedUser(s);

        // RBAC check
        if (u.getRole() == AppRole.ROLE_COLLECTIONS_AGENT || u.getRole() == AppRole.ROLE_COLLECTIONS_MANAGER) {
            throw new AccessDeniedException("Collections staff are not authorized to originate loans.");
        }

        // Validate amount against configured limits
        service.validateLoanAmount(amount);

        // Dynamically resolve applicable slab for this amount from server bank policy
        com.loanproject.loan_app.policy.LoanSlab applicableSlab = service.getSlabForAmount(amount);

        User targetUser;
        double effectiveInterest = applicableSlab.getInterestRate();
        int effectiveTenure = applicableSlab.getTenureMonths();

        if (u.getRole() == AppRole.ROLE_LOAN_OFFICER || u.getRole() == AppRole.ROLE_ADMIN) {
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer email is required for staff loan origination.");
            }
            targetUser = userService.findByEmail(customerEmail.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerEmail));
            if (targetUser.getRole() != AppRole.ROLE_CUSTOMER) {
                throw new AccessDeniedException("Loans can only be originated for customers.");
            }
            if (clientInterest != null && clientInterest > 0) {
                effectiveInterest = clientInterest;
            }
            if (clientMonths != null && clientMonths > 0) {
                effectiveTenure = clientMonths;
            }
        } else if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            // Customer self-service:
            // SECURITY: Never trust interest rate or tenure submitted by client.
            // Client interest & tenure parameters are strictly ignored and server-side policy slab values enforced.
            targetUser = u;
            effectiveInterest = applicableSlab.getInterestRate();
            effectiveTenure = applicableSlab.getTenureMonths();
        } else {
            throw new AccessDeniedException("Unauthorized to originate loans.");
        }

        service.apply(targetUser, amount, effectiveInterest, effectiveTenure);
        return "redirect:/dashboard";
    }

    @GetMapping("/api/loan-policy/slabs")
    @org.springframework.web.bind.annotation.ResponseBody
    public List<com.loanproject.loan_app.policy.LoanSlab> getLoanSlabs() {
        return service.getLoanSlabs();
    }

    @GetMapping("/api/loan-policy/quote")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<?> getLoanQuote(@RequestParam("amount") double amount) {
        try {
            service.validateLoanAmount(amount);
            com.loanproject.loan_app.policy.LoanSlab slab = service.getSlabForAmount(amount);
            double emi = service.getLoanPolicyService() != null ?
                    service.getLoanPolicyService().calculateEmi(amount, slab.getInterestRate(), slab.getTenureMonths()) :
                    0.0;
            java.util.Map<String, Object> res = new java.util.HashMap<>();
            res.put("valid", true);
            res.put("amount", amount);
            res.put("slabName", slab.getSlabName());
            res.put("interestRate", slab.getInterestRate());
            res.put("tenureMonths", slab.getTenureMonths());
            res.put("emi", emi);
            res.put("totalRepayment", Math.round(emi * slab.getTenureMonths() * 100.0) / 100.0);
            return org.springframework.http.ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("valid", false);
            err.put("message", e.getMessage());
            return org.springframework.http.ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/payPage")
    public String payPage(@RequestParam(value = "loanId", required = false) Long loanId,
                          HttpSession s,
                          ModelMap m) {
        User u = getAuthenticatedUser(s);

        // RBAC check: Staff members cannot process repayments (Customer self-service only)
        if (u.getRole() != AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Staff members are not authorized to process repayments. Repayments must be initiated by customers through customer self-service.");
        }

        Loan l;
        if (loanId != null) {
            l = service.getById(loanId)
                    .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

            // Enforce Object-Level Authorization: Customer cannot view another user's loan payment page
            if (u.getRole() == AppRole.ROLE_CUSTOMER && !l.getUser().getId().equals(u.getId())) {
                throw new AccessDeniedException("Forbidden: You cannot access payment information for another customer's loan.");
            }
        } else {
            java.util.List<Loan> userLoans = service.get(u);
            if (userLoans.isEmpty()) {
                m.addAttribute("errorMessage", "No active loans found for repayment.");
                l = null;
            } else if (userLoans.size() == 1) {
                l = userLoans.get(0);
            } else {
                // When multiple loans exist, redirect to dashboard to select specific loan
                return "redirect:/dashboard?selectLoan=true";
            }
        }

        m.addAttribute("loan", l);
        m.addAttribute("currentUser", u);
        m.addAttribute("transactionReference", "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000));
        return "pay";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam("loanId") Long loanId,
                      @RequestParam("amount") double amount,
                      @RequestParam(value = "paymentMode", required = false, defaultValue = "UPI") String paymentMode,
                      @RequestParam(value = "transactionReference", required = false) String transactionReference,
                      HttpSession s) {
        User u = getAuthenticatedUser(s);

        // RBAC check: Staff members cannot process repayments (Customer self-service only)
        if (u.getRole() != AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Staff members are not authorized to process repayments. Repayments must be initiated by customers through customer self-service.");
        }

        Loan l = service.getById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        // Enforce Object-Level Authorization: Customer cannot pay another customer's loan
        if (u.getRole() == AppRole.ROLE_CUSTOMER && !l.getUser().getId().equals(u.getId())) {
            throw new AccessDeniedException("Forbidden: You cannot process payments for another customer's loan.");
        }

        service.pay(loanId, amount, paymentMode, transactionReference);
        return "redirect:/dashboard";
    }

    private java.util.List<com.loanproject.loan_app.dto.CustomerActivityDto> getCustomerRecentActivities(java.util.List<Loan> customerLoans) {
        if (customerLoans == null || customerLoans.isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<com.loanproject.loan_app.dto.CustomerActivityDto> activityList = new java.util.ArrayList<>();

        // 1. Persisted loan application & status events
        for (Loan l : customerLoans) {
            String accNum = l.getAccountNumber() != null && !l.getAccountNumber().isBlank()
                    ? l.getAccountNumber()
                    : String.format("LN%010d", l.getLoanId());
            String prodType = l.getProductType() != null ? l.getProductType() : "Personal Loan";
            String st = l.getStatus() != null ? l.getStatus() : "ACTIVE";

            String title;
            String badge;
            String icon;
            if ("ACTIVE".equalsIgnoreCase(st) || "APPROVED".equalsIgnoreCase(st)) {
                title = "Loan Approved & Active";
                badge = "badge-success";
                icon = "✅";
            } else if ("CLOSED".equalsIgnoreCase(st)) {
                title = "Loan Closed & Settled";
                badge = "badge-secondary";
                icon = "🎉";
            } else if ("REJECTED".equalsIgnoreCase(st)) {
                title = "Application Rejected";
                badge = "badge-danger";
                icon = "❌";
            } else {
                title = "Loan Application Submitted";
                badge = "badge-warning";
                icon = "📝";
            }

            String desc = String.format("%s for ₹%,.2f (Account %s)", prodType, l.getAmount(), accNum);
            java.math.BigDecimal amt = java.math.BigDecimal.valueOf(l.getAmount()).setScale(2, java.math.RoundingMode.HALF_UP);
            java.time.Instant ts = l.getCreatedAt() != null ? l.getCreatedAt() : java.time.Instant.now();

            activityList.add(new com.loanproject.loan_app.dto.CustomerActivityDto(
                    "LOAN_EVENT", title, desc, ts, st, amt, badge, icon));
        }

        // 2. Persisted repayment transaction history (bounded to top 5 to prevent memory bloat)
        try {
            java.util.List<com.loanproject.loan_app.bean.PaymentTransaction> txns =
                    paymentTransactionRepository.findTop5ByLoanInOrderByPaymentDateDesc(customerLoans);
            if (txns != null) {
                for (com.loanproject.loan_app.bean.PaymentTransaction t : txns) {
                    String accNum = t.getLoan() != null && t.getLoan().getAccountNumber() != null
                            ? t.getLoan().getAccountNumber()
                            : (t.getLoan() != null ? "#" + t.getLoan().getLoanId() : "");
                    String title = "Repayment Completed";
                    String desc = String.format("EMI payment of ₹%,.2f processed via %s for Account %s",
                            t.getAmount() != null ? t.getAmount().doubleValue() : 0.0,
                            t.getPaymentMode() != null ? t.getPaymentMode() : "UPI",
                            accNum);
                    activityList.add(new com.loanproject.loan_app.dto.CustomerActivityDto(
                            "REPAYMENT",
                            title,
                            desc,
                            t.getPaymentDate(),
                            t.getStatus() != null ? t.getStatus() : "SUCCESS",
                            t.getAmount(),
                            "badge-success",
                            "💳"
                    ));
                }
            }
        } catch (Exception e) {
            // Graceful degradation in case of transient repository error
        }

        // Sort descending by timestamp, limit to top 5
        return activityList.stream()
                .sorted(java.util.Comparator.comparing(com.loanproject.loan_app.dto.CustomerActivityDto::getTimestamp).reversed())
                .limit(5)
                .toList();
    }

    @PostMapping("/staff/loans/approve")
    public String approveStaffLoan(@RequestParam("loanId") Long loanId,
                                   HttpSession s,
                                   HttpServletRequest request) {
        User u = getAuthenticatedUser(s);
        if (u.getRole() != AppRole.ROLE_LOAN_OFFICER && u.getRole() != AppRole.ROLE_ADMIN) {
            throw new AccessDeniedException("Unauthorized: Only Loan Officers and Admins can approve loan applications.");
        }
        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(u.getBranch()))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        Loan approved = service.approveLoan(loanId, u.getEmail(), branchFilter);
        if (auditService != null) {
            auditService.logLoanApplicationApproved(u.getEmail(), request, loanId, approved.getBranch());
        }
        return "redirect:/dashboard?approved=true";
    }

    @PostMapping("/staff/loans/reject")
    public String rejectStaffLoan(@RequestParam("loanId") Long loanId,
                                  @RequestParam(value = "reason", required = false, defaultValue = "Application does not meet eligibility criteria") String reason,
                                  HttpSession s,
                                  HttpServletRequest request) {
        User u = getAuthenticatedUser(s);
        if (u.getRole() != AppRole.ROLE_LOAN_OFFICER && u.getRole() != AppRole.ROLE_ADMIN) {
            throw new AccessDeniedException("Unauthorized: Only Loan Officers and Admins can reject loan applications.");
        }
        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(u.getBranch()))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        Loan rejected = service.rejectLoan(loanId, reason, u.getEmail(), branchFilter);
        if (auditService != null) {
            auditService.logLoanApplicationRejected(u.getEmail(), request, loanId, reason, rejected.getBranch());
        }
        return "redirect:/dashboard?rejected=true";
    }

    @PostMapping("/staff/collections/record")
    public String recordStaffCollection(@RequestParam("loanId") Long loanId,
                                        @RequestParam("amount") double amount,
                                        @RequestParam(value = "paymentMode", required = false, defaultValue = "CASH") String paymentMode,
                                        @RequestParam(value = "transactionReference", required = false) String transactionReference,
                                        HttpSession s,
                                        HttpServletRequest request) {
        User u = getAuthenticatedUser(s);
        if (u.getRole() != AppRole.ROLE_COLLECTIONS_AGENT && u.getRole() != AppRole.ROLE_COLLECTIONS_MANAGER && u.getRole() != AppRole.ROLE_ADMIN) {
            throw new AccessDeniedException("Unauthorized: Role not permitted to record collections.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Collection payment amount must be greater than zero.");
        }

        Loan loan = service.getById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        // Enforce branch authorization
        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN || "ALL".equalsIgnoreCase(u.getBranch()))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        if (!"ALL".equalsIgnoreCase(branchFilter) && !branchFilter.equalsIgnoreCase(loan.getBranch())) {
            if (auditService != null) {
                auditService.logCollectionDenied(u.getEmail(), request, loanId,
                        "Officer branch " + branchFilter + " unauthorized for loan branch " + loan.getBranch());
            }
            throw new AccessDeniedException("Unauthorized: Loan belongs to another branch.");
        }

        if (amount > loan.getRemaining()) {
            throw new IllegalArgumentException("Collection amount (₹" + amount + ") cannot exceed outstanding balance of ₹" + loan.getRemaining());
        }

        String txRef = (transactionReference != null && !transactionReference.isBlank())
                ? transactionReference.trim()
                : "COL" + System.currentTimeMillis() + (int) (Math.random() * 1000);

        String mode = (paymentMode != null && !paymentMode.isBlank()) ? paymentMode.trim().toUpperCase() : "CASH";

        service.pay(loanId, amount, mode, txRef);

        if (auditService != null) {
            auditService.logCollectionRecorded(u.getEmail(), request, loanId, amount, mode, txRef, loan.getBranch());
        }

        return "redirect:/dashboard?collectionRecorded=true";
    }

    private java.util.Map<Long, java.util.List<com.loanproject.loan_app.bean.RepaymentInstallment>> preloadInstallmentsByLoanId(List<Loan> loans) {
        if (installmentRepository == null || loans == null || loans.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        try {
            return installmentRepository.findAll().stream()
                    .filter(r -> r.getLoan() != null && r.getLoan().getLoanId() != null)
                    .collect(java.util.stream.Collectors.groupingBy(r -> r.getLoan().getLoanId()));
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        }
    }

    private com.loanproject.loan_app.dto.StaffDashboardLoanDto mapToStaffLoanDto(Loan l) {
        return mapToStaffLoanDto(l, null);
    }

    private com.loanproject.loan_app.dto.StaffDashboardLoanDto mapToStaffLoanDto(
            Loan l, java.util.Map<Long, java.util.List<com.loanproject.loan_app.bean.RepaymentInstallment>> preloadedInstallments) {
        String borrowerName = l.getUser() != null ? l.getUser().getName() : "Unknown";
        String rawEmail = l.getUser() != null ? l.getUser().getEmail() : "";
        String maskedEmail = com.loanproject.loan_app.security.DataMaskingUtils.maskEmail(rawEmail);
        String customerId = l.getUser() != null && l.getUser().getCustomerId() != null
                ? l.getUser().getCustomerId()
                : (l.getUser() != null ? "CUST" + l.getUser().getId() : "N/A");
        String maskedPhone = l.getUser() != null ? com.loanproject.loan_app.security.DataMaskingUtils.maskPhone(l.getUser().getPhoneNumber()) : "N/A";
        String createdAt = l.getCreatedAt() != null
                ? java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(java.time.ZoneId.systemDefault()).format(l.getCreatedAt())
                : "N/A";

        String repaymentStatus = "CURRENT";
        int dpd = 0;
        double overdueAmount = 0.0;

        String loanStatus = l.getStatus() != null ? l.getStatus() : "ACTIVE";
        if ("CLOSED".equalsIgnoreCase(loanStatus)) {
            repaymentStatus = "CLOSED";
        } else if ("REJECTED".equalsIgnoreCase(loanStatus)) {
            repaymentStatus = "REJECTED";
        } else if ("PENDING".equalsIgnoreCase(loanStatus) || "SUBMITTED".equalsIgnoreCase(loanStatus)
                || "IN_REVIEW".equalsIgnoreCase(loanStatus) || "APPLIED".equalsIgnoreCase(loanStatus)) {
            repaymentStatus = "PENDING_APPROVAL";
        } else {
            if (l.getRemaining() <= 0) {
                repaymentStatus = "CLOSED";
            } else if (installmentRepository != null) {
                java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.systemDefault());
                java.util.List<com.loanproject.loan_app.bean.RepaymentInstallment> installments;
                if (preloadedInstallments != null) {
                    installments = preloadedInstallments.getOrDefault(l.getLoanId(), java.util.Collections.emptyList());
                } else {
                    installments = installmentRepository.findByLoan_LoanIdOrderByInstallmentNumberAsc(l.getLoanId());
                }
                int maxDpd = 0;
                double totalOverdue = 0.0;
                for (com.loanproject.loan_app.bean.RepaymentInstallment inst : installments) {
                    if ("OVERDUE".equalsIgnoreCase(inst.getStatus()) ||
                            ("PARTIALLY_PAID".equalsIgnoreCase(inst.getStatus()) && inst.getDpd() > 0) ||
                            (inst.getDueDate() != null && inst.getDueDate().isBefore(today) &&
                                    (inst.getPaidAmount() == null || inst.getPaidAmount().compareTo(inst.getEmiAmount()) < 0))) {
                        int calculatedDpd = inst.getDpd() > 0 ? inst.getDpd() :
                                (inst.getDueDate() != null ? Math.max(0, (int) java.time.temporal.ChronoUnit.DAYS.between(inst.getDueDate(), today)) : 0);
                        if (calculatedDpd > maxDpd) {
                            maxDpd = calculatedDpd;
                        }
                        java.math.BigDecimal emi = inst.getEmiAmount() != null ? inst.getEmiAmount() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal paid = inst.getPaidAmount() != null ? inst.getPaidAmount() : java.math.BigDecimal.ZERO;
                        java.math.BigDecimal unpaid = emi.subtract(paid).max(java.math.BigDecimal.ZERO);
                        totalOverdue += unpaid.doubleValue();
                    }
                }
                if (maxDpd > 0 || totalOverdue > 0) {
                    repaymentStatus = "OVERDUE";
                    dpd = maxDpd;
                    overdueAmount = Math.round(totalOverdue * 100.0) / 100.0;
                }
            }
        }

        com.loanproject.loan_app.dto.StaffDashboardLoanDto dto = new com.loanproject.loan_app.dto.StaffDashboardLoanDto(
                l.getLoanId(),
                l.getAccountNumber() != null ? l.getAccountNumber() : String.format("LN%010d", l.getLoanId()),
                borrowerName,
                maskedEmail,
                l.getAmount(),
                l.getInterest(),
                l.getMonths(),
                l.getEmi(),
                l.getRemaining(),
                loanStatus,
                l.getBranch() != null ? l.getBranch() : "MAIN",
                repaymentStatus,
                dpd,
                overdueAmount,
                customerId,
                maskedPhone,
                createdAt
        );
        dto.setProductType(l.getProductType() != null ? l.getProductType() : "Personal Loan");
        String startOrDisbursementDate = l.getDisbursementDate() != null
                ? l.getDisbursementDate().toString()
                : (l.getCreatedAt() != null ? java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(java.time.ZoneId.systemDefault()).format(l.getCreatedAt()) : "N/A");
        dto.setStartDate(startOrDisbursementDate);
        dto.setBorrowerName(borrowerName);
        dto.setRawEmail(rawEmail);
        dto.setRawPhone(l.getUser() != null ? l.getUser().getPhoneNumber() : "");
        dto.setUserId(l.getUser() != null ? l.getUser().getId() : null);
        return dto;
    }

    public List<com.loanproject.loan_app.dto.ActiveBorrowerDto> buildActiveBorrowers(List<Loan> authorizedLoans, User currentStaff) {
        if (authorizedLoans == null || authorizedLoans.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // Filter active loans only
        List<Loan> activeLoans = authorizedLoans.stream()
                .filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()))
                .toList();

        // Group by borrower identifier key to avoid duplicate records
        java.util.Map<String, List<Loan>> borrowerGroups = new java.util.LinkedHashMap<>();
        for (Loan l : activeLoans) {
            String key;
            if (l.getUser() != null && l.getUser().getId() != null) {
                key = "UID_" + l.getUser().getId();
            } else if (l.getUser() != null && l.getUser().getCustomerId() != null && !l.getUser().getCustomerId().isBlank()) {
                key = "CUST_" + l.getUser().getCustomerId().trim();
            } else if (l.getUser() != null && l.getUser().getEmail() != null && !l.getUser().getEmail().isBlank()) {
                key = "EMAIL_" + l.getUser().getEmail().trim().toLowerCase();
            } else {
                key = "LOAN_" + l.getLoanId();
            }
            borrowerGroups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(l);
        }

        boolean canViewFullContact = currentStaff != null && 
                (currentStaff.getRole() == AppRole.ROLE_ADMIN || currentStaff.getRole() == AppRole.ROLE_LOAN_OFFICER);

        List<com.loanproject.loan_app.dto.ActiveBorrowerDto> result = new java.util.ArrayList<>();
        for (List<Loan> loans : borrowerGroups.values()) {
            if (loans.isEmpty()) continue;
            Loan first = loans.get(0);
            User u = first.getUser();

            String borrowerName = u != null ? u.getName() : "Unknown";
            String customerId = (u != null && u.getCustomerId() != null && !u.getCustomerId().isBlank())
                    ? u.getCustomerId()
                    : (u != null && u.getId() != null ? "CUST" + u.getId() : "CUST" + first.getLoanId());
            String rawEmail = u != null ? u.getEmail() : "";
            String rawPhone = u != null ? u.getPhoneNumber() : "";

            String displayEmail = canViewFullContact ? rawEmail : com.loanproject.loan_app.security.DataMaskingUtils.maskEmail(rawEmail);
            String displayPhone = canViewFullContact ? rawPhone : com.loanproject.loan_app.security.DataMaskingUtils.maskPhone(rawPhone);
            String contactInfo = displayEmail + (displayPhone != null && !displayPhone.isBlank() && !"N/A".equals(displayPhone) ? " | " + displayPhone : "");

            List<String> accNums = new java.util.ArrayList<>();
            double totalSanctioned = 0.0;
            double totalOutstanding = 0.0;
            String branch = first.getBranch() != null ? first.getBranch() : "MAIN";

            for (Loan l : loans) {
                String acc = l.getAccountNumber() != null ? l.getAccountNumber() : String.format("LN%010d", l.getLoanId());
                if (!accNums.contains(acc)) {
                    accNums.add(acc);
                }
                totalSanctioned += l.getAmount();
                totalOutstanding += l.getRemaining();
                if (l.getBranch() != null && !l.getBranch().isBlank()) {
                    branch = l.getBranch();
                }
            }

            com.loanproject.loan_app.dto.ActiveBorrowerDto dto = new com.loanproject.loan_app.dto.ActiveBorrowerDto(
                    u != null ? u.getId() : null,
                    customerId,
                    borrowerName,
                    accNums,
                    loans.size(),
                    Math.round(totalSanctioned * 100.0) / 100.0,
                    Math.round(totalOutstanding * 100.0) / 100.0,
                    branch,
                    displayEmail,
                    displayPhone,
                    contactInfo,
                    "ACTIVE",
                    first.getLoanId()
            );
            result.add(dto);
        }

        result.sort(java.util.Comparator.comparing(com.loanproject.loan_app.dto.ActiveBorrowerDto::getBorrowerName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @GetMapping({"/total-loans", "/all-loans", "/staff/loans", "/staff/total-loans"})
    public String viewTotalLoans(@RequestParam(value = "branch", required = false) String requestedBranch,
                                 @RequestParam(value = "status", required = false) String requestedStatus,
                                 HttpSession s, ModelMap m) {
        User u = getAuthenticatedUser(s);
        if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            return "redirect:/dashboard";
        }

        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN && ("ALL".equalsIgnoreCase(u.getBranch()) || u.getBranch() == null))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        List<Loan> rawBranchLoans = service.getAll().stream()
                .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                .toList();

        java.util.Map<Long, java.util.List<com.loanproject.loan_app.bean.RepaymentInstallment>> preloadedInstallments =
                preloadInstallmentsByLoanId(rawBranchLoans);

        List<com.loanproject.loan_app.dto.StaffDashboardLoanDto> staffLoans = rawBranchLoans.stream()
                .map(l -> mapToStaffLoanDto(l, preloadedInstallments))
                .toList();

        List<String> allBranches = staffLoans.stream()
                .map(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getBranch)
                .filter(b -> b != null && !b.isBlank())
                .distinct()
                .sorted()
                .toList();

        List<String> allStatuses = staffLoans.stream()
                .map(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getStatus)
                .filter(st -> st != null && !st.isBlank())
                .distinct()
                .sorted()
                .toList();

        m.addAttribute("loans", staffLoans);
        m.addAttribute("totalLoansCount", staffLoans.size());
        m.addAttribute("allBranches", allBranches);
        m.addAttribute("allStatuses", allStatuses);
        m.addAttribute("currentUser", u);
        m.addAttribute("userRole", u.getRole().name());
        m.addAttribute("officerBranch", branchFilter);
        m.addAttribute("selectedBranch", requestedBranch);
        m.addAttribute("selectedStatus", requestedStatus);
        return "total-loans";
    }

    @GetMapping({"/active-borrowers", "/staff/active-borrowers", "/staff/borrowers"})
    public String viewActiveBorrowers(@RequestParam(value = "branch", required = false) String requestedBranch,
                                      HttpSession s, ModelMap m) {
        User u = getAuthenticatedUser(s);
        if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            return "redirect:/dashboard";
        }

        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN && ("ALL".equalsIgnoreCase(u.getBranch()) || u.getBranch() == null))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        List<Loan> rawBranchLoans = service.getAll().stream()
                .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                .toList();

        List<com.loanproject.loan_app.dto.ActiveBorrowerDto> activeBorrowers = buildActiveBorrowers(rawBranchLoans, u);

        List<String> allBranches = activeBorrowers.stream()
                .map(com.loanproject.loan_app.dto.ActiveBorrowerDto::getBranch)
                .filter(b -> b != null && !b.isBlank())
                .distinct()
                .sorted()
                .toList();

        m.addAttribute("activeBorrowers", activeBorrowers);
        m.addAttribute("activeBorrowersCount", activeBorrowers.size());
        m.addAttribute("allBranches", allBranches);
        m.addAttribute("currentUser", u);
        m.addAttribute("userRole", u.getRole().name());
        m.addAttribute("officerBranch", branchFilter);
        m.addAttribute("selectedBranch", requestedBranch);
        return "active-borrowers";
    }

    @GetMapping({"/api/staff/total-loans", "/api/staff/all-loans", "/api/staff/loans"})
    @ResponseBody
    public ResponseEntity<List<com.loanproject.loan_app.dto.StaffDashboardLoanDto>> apiTotalLoans(HttpSession s) {
        User u = getAuthenticatedUser(s);
        if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Unauthorized: Customers cannot access staff loan lists.");
        }

        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN && ("ALL".equalsIgnoreCase(u.getBranch()) || u.getBranch() == null))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        List<com.loanproject.loan_app.dto.StaffDashboardLoanDto> staffLoans = service.getAll().stream()
                .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                .map(this::mapToStaffLoanDto)
                .toList();

        return ResponseEntity.ok(staffLoans);
    }

    @GetMapping({"/api/staff/active-borrowers", "/api/staff/borrowers"})
    @ResponseBody
    public ResponseEntity<List<com.loanproject.loan_app.dto.ActiveBorrowerDto>> apiActiveBorrowers(HttpSession s) {
        User u = getAuthenticatedUser(s);
        if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Unauthorized: Customers cannot access staff borrower lists.");
        }

        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN && ("ALL".equalsIgnoreCase(u.getBranch()) || u.getBranch() == null))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        List<Loan> rawBranchLoans = service.getAll().stream()
                .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                .toList();

        List<com.loanproject.loan_app.dto.ActiveBorrowerDto> activeBorrowers = buildActiveBorrowers(rawBranchLoans, u);
        return ResponseEntity.ok(activeBorrowers);
    }

    @GetMapping("/api/staff/dashboard-summary")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> apiDashboardSummary(HttpSession s) {
        User u = getAuthenticatedUser(s);
        if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            throw new AccessDeniedException("Unauthorized: Customers cannot access staff dashboard summary.");
        }

        String branchFilter = (u.getRole() == AppRole.ROLE_ADMIN && ("ALL".equalsIgnoreCase(u.getBranch()) || u.getBranch() == null))
                ? "ALL"
                : (u.getBranch() != null ? u.getBranch() : "MAIN");

        List<Loan> rawBranchLoans = service.getAll().stream()
                .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                .toList();

        java.util.Map<Long, java.util.List<com.loanproject.loan_app.bean.RepaymentInstallment>> preloadedInstallments =
                preloadInstallmentsByLoanId(rawBranchLoans);

        List<com.loanproject.loan_app.dto.StaffDashboardLoanDto> staffLoans = rawBranchLoans.stream()
                .map(l -> mapToStaffLoanDto(l, preloadedInstallments))
                .toList();

        List<com.loanproject.loan_app.dto.ActiveBorrowerDto> activeBorrowers = buildActiveBorrowers(rawBranchLoans, u);

        int totalLoansCount = staffLoans.size();
        int activeLoansCount = (int) staffLoans.stream().filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus())).count();
        int activeBorrowersCount = activeBorrowers.size();
        int pendingLoansCount = (int) staffLoans.stream().filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())
                || "SUBMITTED".equalsIgnoreCase(l.getStatus())
                || "IN_REVIEW".equalsIgnoreCase(l.getStatus())
                || "APPLIED".equalsIgnoreCase(l.getStatus())).count();
        double totalPrincipal = staffLoans.stream().mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getAmount).sum();
        double totalRemaining = staffLoans.stream().mapToDouble(com.loanproject.loan_app.dto.StaffDashboardLoanDto::getRemaining).sum();

        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalLoansCount", totalLoansCount);
        summary.put("activeLoansCount", activeLoansCount);
        summary.put("activeBorrowersCount", activeBorrowersCount);
        summary.put("pendingLoansCount", pendingLoansCount);
        summary.put("totalPrincipal", totalPrincipal);
        summary.put("totalRemaining", totalRemaining);
        summary.put("branch", branchFilter);
        return ResponseEntity.ok(summary);
    }
}
