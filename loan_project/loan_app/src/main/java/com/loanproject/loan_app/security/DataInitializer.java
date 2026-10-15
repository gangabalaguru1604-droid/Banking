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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${security.initial-admin.email:admin@bank.com}")
    private String adminEmail;

    @Value("${security.initial-admin.password:Admin@Bank123!}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AesGcmEncryptionService encryptionService,
                           TotpService totpService,
                           org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionService = encryptionService;
        this.totpService = totpService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        migrateExistingDatabaseColumns();

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

    private void migrateExistingDatabaseColumns() {
        try {
            // Ensure columns exist on PostgreSQL/H2 tables (users)
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'ROLE_CUSTOMER'");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS user_type VARCHAR(50) DEFAULT 'CUSTOMER'");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_attempt_count INT DEFAULT 0");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS account_non_locked BOOLEAN DEFAULT TRUE");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS lock_time TIMESTAMP NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_enrolled BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(512) NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS mfa_type VARCHAR(20) DEFAULT 'TOTP'");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(50) NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS branch VARCHAR(50) DEFAULT 'MAIN'");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS customer_id VARCHAR(50) NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS pan_number VARCHAR(20) NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS aadhaar_number VARCHAR(20) NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS date_of_birth DATE NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(30) DEFAULT 'VERIFIED'");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(255) NULL");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_contact_method VARCHAR(30) DEFAULT 'EMAIL'");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_detail_update_required BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE users SET contact_detail_update_required = FALSE WHERE contact_detail_update_required IS NULL");

            // Ensure columns exist on PostgreSQL/H2 tables (loan)
            jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN IF NOT EXISTS account_number VARCHAR(50) NULL");
            jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN IF NOT EXISTS branch VARCHAR(50) DEFAULT 'MAIN'");
            jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE'");
            jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NULL");
            jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN IF NOT EXISTS product_type VARCHAR(50) DEFAULT 'Personal Loan'");
            jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN IF NOT EXISTS interest_type VARCHAR(20) DEFAULT 'FIXED'");
            jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN IF NOT EXISTS disbursed_amount NUMERIC(15,2) NULL");
            jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN IF NOT EXISTS disbursement_date DATE NULL");

            // Ensure users_seq sequence exists and is in sync with max(id)
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 50 INCREMENT BY 50");
            try {
                jdbcTemplate.execute("SELECT setval('users_seq', (SELECT COALESCE(MAX(id), 1) + 50 FROM users))");
            } catch (Exception ignored) {}

            // Populate default values for any pre-existing rows that had nulls
            jdbcTemplate.execute("UPDATE users SET role = 'ROLE_CUSTOMER' WHERE role IS NULL");
            jdbcTemplate.execute("UPDATE users SET user_type = 'CUSTOMER' WHERE user_type IS NULL");
            jdbcTemplate.execute("UPDATE users SET account_non_locked = true WHERE account_non_locked IS NULL");
            jdbcTemplate.execute("UPDATE users SET failed_attempt_count = 0 WHERE failed_attempt_count IS NULL");
            jdbcTemplate.execute("UPDATE users SET mfa_enrolled = false WHERE mfa_enrolled IS NULL");
            jdbcTemplate.execute("UPDATE users SET mfa_type = 'TOTP' WHERE mfa_type IS NULL");
            jdbcTemplate.execute("UPDATE users SET branch = 'MAIN' WHERE branch IS NULL");
            jdbcTemplate.execute("UPDATE users SET branch = 'ALL' WHERE role = 'ROLE_ADMIN' AND (branch IS NULL OR branch = 'MAIN')");
            jdbcTemplate.execute("UPDATE users SET kyc_status = 'VERIFIED' WHERE kyc_status IS NULL");
            jdbcTemplate.execute("UPDATE users SET preferred_contact_method = 'EMAIL' WHERE preferred_contact_method IS NULL");
            jdbcTemplate.execute("UPDATE users SET date_of_birth = '1990-05-15', address = '42 MG Road, Bengaluru, Karnataka - 560001' WHERE date_of_birth IS NULL AND user_type = 'CUSTOMER'");

            // Backfill customer ID for customer accounts (CUST + 6 digits)
            jdbcTemplate.execute("UPDATE users SET customer_id = 'CUST' || LPAD(id::text, 6, '0') WHERE customer_id IS NULL AND user_type = 'CUSTOMER'");

            // Backfill sample KYC profile fields for pre-existing test customers if empty
            jdbcTemplate.execute("UPDATE users SET phone_number = '9876543210', pan_number = 'ABCDE1234F', aadhaar_number = '123456789012' WHERE email = 'gangab0934@gmail.com' AND pan_number IS NULL");
            jdbcTemplate.execute("UPDATE users SET phone_number = '9876543211', pan_number = 'XYZPK5678Q', aadhaar_number = '987654321098' WHERE email = 'gangabalaguru1604@gmail.com' AND pan_number IS NULL");

            // Backfill loan table
            jdbcTemplate.execute("UPDATE loan SET branch = 'MAIN' WHERE branch IS NULL");
            jdbcTemplate.execute("UPDATE loan SET status = 'ACTIVE' WHERE status IS NULL");
            jdbcTemplate.execute("UPDATE loan SET account_number = 'LN' || LPAD(loan_id::text, 10, '0') WHERE account_number IS NULL");
            jdbcTemplate.execute("UPDATE loan SET product_type = 'Personal Loan' WHERE product_type IS NULL");
            jdbcTemplate.execute("UPDATE loan SET interest_type = 'FIXED' WHERE interest_type IS NULL");
            jdbcTemplate.execute("UPDATE loan SET disbursed_amount = amount WHERE disbursed_amount IS NULL OR disbursed_amount = 0");
            jdbcTemplate.execute("UPDATE loan SET disbursement_date = CURRENT_DATE WHERE disbursement_date IS NULL");

            // Indexes for fast lookup (< 3s target)
            try {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_loan_acc_num ON loan(account_number)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_loan_branch ON loan(branch)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_cust_id ON users(customer_id)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number)");
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_branch ON users(branch)");
            } catch (Exception ignored) {}

        } catch (Exception e) {
            log.debug("Database migration info: {}", e.getMessage());
        }
    }
}
