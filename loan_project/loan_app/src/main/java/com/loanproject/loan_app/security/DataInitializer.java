package com.loanproject.loan_app.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesGcmEncryptionService encryptionService;
    private final TotpService totpService;

    @Value("${security.initial-admin.email:admin@bank.com}")
    private String adminEmail;

    @Value("${security.initial-admin.password:Admin@Bank123!}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AesGcmEncryptionService encryptionService,
                           TotpService totpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.totpService = totpService;
    }

    @Override
    public void run(String... args) throws Exception {
        String seedSecret = "JBSWY3DPEHPK3PXP";
        Optional<User> optionalAdmin = userRepository.findByEmail(adminEmail.trim().toLowerCase());
        if (optionalAdmin.isEmpty()) {
            User admin = new User();
            admin.setName("System Administrator");
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(AppRole.ROLE_ADMIN);
            admin.setUserType(UserType.STAFF);
            admin.setAccountNonLocked(true);
            admin.setFailedAttemptCount(0);

            admin.setMfaSecret(encryptionService.encrypt(seedSecret));
            admin.setMfaEnrolled(true);
            admin.setMfaType("STAFF_STATIC");
            admin.setBranch("ALL");

            userRepository.save(admin);
            log.info("Initialized default administrator: {} (MFA Secret: {})", adminEmail, seedSecret);
        } else {
            User admin = optionalAdmin.get();
            healSecretIfNeeded(admin, seedSecret, "STAFF_STATIC");
        }

        // Ensure default staff accounts for development
        ensureStaffAccount("Loan Officer", "officer@bank.com", "Officer@Bank123!", AppRole.ROLE_LOAN_OFFICER, "MAIN");
        ensureStaffAccount("Collections Agent", "collector@bank.com", "Collector@Bank123!", AppRole.ROLE_COLLECTIONS_AGENT, "MAIN");
        ensureStaffAccount("Collections Manager", "manager@bank.com", "Manager@Bank123!", AppRole.ROLE_COLLECTIONS_MANAGER, "MAIN");

        // Ensure default customer account for MFA testing
        ensureCustomerAccount("Demo Customer", "customer@bank.com", "Customer@Bank123!", "MAIN");
    }

    private void healSecretIfNeeded(User user, String seedSecret, String mfaType) {
        boolean needHeal = !user.isMfaEnrolled() || user.getMfaSecret() == null || user.getMfaSecret().isBlank();
        if (!needHeal) {
            try {
                String decrypted = encryptionService.decrypt(user.getMfaSecret());
                if (decrypted == null || decrypted.isBlank()) {
                    needHeal = true;
                }
            } catch (Exception e) {
                needHeal = true;
            }
        }
        if (needHeal) {
            user.setMfaSecret(encryptionService.encrypt(seedSecret));
            user.setMfaEnrolled(true);
            if (mfaType != null) {
                user.setMfaType(mfaType);
            }
            userRepository.save(user);
            log.info("Self-healed un-decryptable MFA secret for account: {}", user.getEmail());
        }
    }

    private void ensureStaffAccount(String name, String email, String password, AppRole role, String branch) {
        String cleanEmail = email.trim().toLowerCase();
        Optional<User> optionalStaff = userRepository.findByEmail(cleanEmail);
        String seedSecret = "JBSWY3DPEHPK3PXP";

        if (optionalStaff.isEmpty()) {
            User staff = new User();
            staff.setName(name);
            staff.setEmail(cleanEmail);
            staff.setPassword(passwordEncoder.encode(password));
            staff.setRole(role);
            staff.setUserType(UserType.STAFF);
            staff.setAccountNonLocked(true);
            staff.setFailedAttemptCount(0);
            staff.setBranch(branch != null ? branch : "MAIN");
            staff.setMfaSecret(encryptionService.encrypt(seedSecret));
            staff.setMfaEnrolled(true);
            staff.setMfaType("STAFF_STATIC");

            userRepository.save(staff);
            log.info("Initialized staff account: {} with role {} at branch {}", cleanEmail, role, branch);
        } else {
            User staff = optionalStaff.get();
            healSecretIfNeeded(staff, seedSecret, "STAFF_STATIC");
        }
    }

    private void ensureCustomerAccount(String name, String email, String password, String branch) {
        String cleanEmail = email.trim().toLowerCase();
        Optional<User> optionalCustomer = userRepository.findByEmail(cleanEmail);

        if (optionalCustomer.isEmpty()) {
            User customer = new User();
            customer.setName(name);
            customer.setEmail(cleanEmail);
            customer.setPassword(passwordEncoder.encode(password));
            customer.setRole(AppRole.ROLE_CUSTOMER);
            customer.setUserType(UserType.CUSTOMER);
            customer.setAccountNonLocked(true);
            customer.setFailedAttemptCount(0);
            customer.setBranch(branch != null ? branch : "MAIN");
            customer.setCustomerId("CUST" + String.format("%04d", (int)(Math.random() * 9000) + 1000));
            customer.setPhoneNumber("9876543210");
            customer.setMfaType("TOTP");
            customer.setMfaEnrolled(false);
            customer.setMfaSecret(null);

            userRepository.save(customer);
            log.info("Initialized default customer account: {} ready for Authenticator QR code scan", cleanEmail);
        } else {
            User customer = optionalCustomer.get();
            // Reset customer to unenrolled TOTP so user can scan QR code with Google Authenticator
            customer.setMfaType("TOTP");
            customer.setMfaEnrolled(false);
            customer.setMfaSecret(null);
            userRepository.save(customer);
            log.info("Reset customer account {} to unenrolled TOTP so user can scan Authenticator QR code", cleanEmail);
        }
    }

}
