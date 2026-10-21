package com.loanproject.loan_app.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
        if (userRepository.findByRole(AppRole.ROLE_ADMIN).isEmpty()) {
            User admin = new User();
            admin.setName("System Administrator");
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(AppRole.ROLE_ADMIN);
            admin.setUserType(UserType.STAFF);
            admin.setAccountNonLocked(true);
            admin.setFailedAttemptCount(0);

            // Known seed secret for default admin: Base32 "JBSWY3DPEHPK3PXP"
            String seedSecret = "JBSWY3DPEHPK3PXP";
            admin.setMfaSecret(encryptionService.encrypt(seedSecret));
            admin.setMfaEnrolled(true);
            admin.setMfaType("TOTP");
            admin.setBranch("ALL");

            userRepository.save(admin);
            log.info("Initialized default administrator: {} (MFA Secret: {})", adminEmail, seedSecret);
        }

        // Seed default staff accounts for development if not present
        seedStaffIfMissing("Loan Officer", "officer@bank.com", "Officer@Bank123!", AppRole.ROLE_LOAN_OFFICER, "MAIN");
        seedStaffIfMissing("Collections Agent", "collector@bank.com", "Collector@Bank123!", AppRole.ROLE_COLLECTIONS_AGENT, "MAIN");
        seedStaffIfMissing("Collections Manager", "manager@bank.com", "Manager@Bank123!", AppRole.ROLE_COLLECTIONS_MANAGER, "MAIN");
    }

    private void seedStaffIfMissing(String name, String email, String password, AppRole role, String branch) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User staff = new User();
            staff.setName(name);
            staff.setEmail(email.trim().toLowerCase());
            staff.setPassword(passwordEncoder.encode(password));
            staff.setRole(role);
            staff.setUserType(UserType.STAFF);
            staff.setAccountNonLocked(true);
            staff.setFailedAttemptCount(0);
            staff.setBranch(branch != null ? branch : "MAIN");

            // Staff starts unenrolled so they complete QR code enrollment on first login
            staff.setMfaSecret(null);
            staff.setMfaEnrolled(false);
            staff.setMfaType("TOTP");

            userRepository.save(staff);
            log.info("Initialized staff account: {} with role {} at branch {}", email, role, branch);
        }
    }

}
