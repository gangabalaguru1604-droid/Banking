package com.loanproject.loan_app.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.Loan;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.security.CustomUserDetails;
import com.loanproject.loan_app.services.LoanService;
import com.loanproject.loan_app.services.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoanController {

    @Autowired
    private LoanService service;

    @Autowired
    private UserService userService;

    @Autowired
    private com.loanproject.loan_app.repository.PaymentTransactionRepository paymentTransactionRepository;

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

            java.util.List<com.loanproject.loan_app.dto.StaffDashboardLoanDto> staffLoans = service.getAll().stream()
                    .filter(l -> "ALL".equals(branchFilter) || branchFilter.equalsIgnoreCase(l.getBranch()))
                    .map(l -> {
                        String borrowerName = l.getUser() != null ? l.getUser().getName() : "Unknown";
                        String rawEmail = l.getUser() != null ? l.getUser().getEmail() : "";
                        String maskedEmail = com.loanproject.loan_app.security.DataMaskingUtils.maskEmail(rawEmail);
                        return new com.loanproject.loan_app.dto.StaffDashboardLoanDto(
                                l.getLoanId(),
                                l.getAccountNumber() != null ? l.getAccountNumber() : String.format("LN%010d", l.getLoanId()),
                                borrowerName,
                                maskedEmail,
                                l.getAmount(),
                                l.getInterest(),
                                l.getMonths(),
                                l.getEmi(),
                                l.getRemaining(),
                                l.getStatus() != null ? l.getStatus() : "ACTIVE",
                                l.getBranch() != null ? l.getBranch() : "MAIN"
                        );
                    })
                    .toList();

            m.addAttribute("loans", staffLoans);
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
        return "applyLoan";
    }

    @PostMapping("/applyLoan")
    public String apply(HttpSession s,
                        @RequestParam(value = "amount") double amount,
                        @RequestParam(value = "interest") double interest,
                        @RequestParam(value = "months") int months,
                        @RequestParam(value = "customerEmail", required = false) String customerEmail) {
        User u = getAuthenticatedUser(s);

        // RBAC check
        if (u.getRole() == AppRole.ROLE_COLLECTIONS_AGENT || u.getRole() == AppRole.ROLE_COLLECTIONS_MANAGER) {
            throw new AccessDeniedException("Collections staff are not authorized to originate loans.");
        }

        User targetUser;
        if (u.getRole() == AppRole.ROLE_LOAN_OFFICER || u.getRole() == AppRole.ROLE_ADMIN) {
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer email is required for staff loan origination.");
            }
            targetUser = userService.findByEmail(customerEmail.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerEmail));
            if (targetUser.getRole() != AppRole.ROLE_CUSTOMER) {
                throw new AccessDeniedException("Loans can only be originated for customers.");
            }
        } else if (u.getRole() == AppRole.ROLE_CUSTOMER) {
            // Customer applying for self
            targetUser = u;
        } else {
            throw new AccessDeniedException("Unauthorized to originate loans.");
        }

        service.apply(targetUser, amount, interest, months);
        return "redirect:/dashboard";
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

        // 2. Persisted repayment transaction history
        try {
            java.util.List<com.loanproject.loan_app.bean.PaymentTransaction> txns =
                    paymentTransactionRepository.findByLoanInOrderByPaymentDateDesc(customerLoans);
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
}
