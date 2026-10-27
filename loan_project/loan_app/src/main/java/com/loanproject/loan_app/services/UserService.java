package com.loanproject.loan_app.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AesGcmEncryptionService;
import com.loanproject.loan_app.security.TotpService;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final AesGcmEncryptionService encryptionService;

    public UserService(UserRepository repo,
                       PasswordEncoder passwordEncoder,
                       TotpService totpService,
                       AesGcmEncryptionService encryptionService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.encryptionService = encryptionService;
    }

    // Password strength regex: min 8 characters, at least 1 uppercase, 1 lowercase, 1 number, 1 special char
    private static final java.util.regex.Pattern PASSWORD_POLICY =
            java.util.regex.Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-={}\\[\\]|:;\"'<>,.~`]).{8,64}$");

    @Transactional
    public void register(User u) {
        if (repo.existsByEmail(u.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (u.getPassword() == null || !PASSWORD_POLICY.matcher(u.getPassword()).matches()) {
            throw new IllegalArgumentException(
                    "Password must be 8-64 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }

        // Public registration ONLY provisions customer accounts. Never staff or admin!
        u.setRole(AppRole.ROLE_CUSTOMER);
        u.setUserType(UserType.CUSTOMER);
        u.setAccountNonLocked(true);
        u.setFailedAttemptCount(0);

        // Adaptive password hashing
        u.setPassword(passwordEncoder.encode(u.getPassword()));

        // Unenrolled initially: MFA enrollment is completed on first login with QR code
        u.setMfaSecret(null);
        u.setMfaEnrolled(false);
        u.setMfaType("TOTP");
        if (u.getBranch() == null || u.getBranch().isBlank()) {
            u.setBranch("MAIN");
        }

        User saved = repo.save(u);
        if (saved.getCustomerId() == null || saved.getCustomerId().isBlank()) {
            saved.setCustomerId(String.format("CUST%06d", saved.getId()));
            saved = repo.save(saved);
        }
        log.info("Registered customer account: {} (pending MFA enrollment)", saved.getEmail());
    }

    public User login(String e, String p) {
        // Kept for backward compatibility, but Spring Security handles authentication
        return repo.findByEmailAndPassword(e, p);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return repo.findByEmail(email.trim().toLowerCase());
    }

    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }

    public List<User> findAllStaff() {
        return repo.findByUserType(UserType.STAFF);
    }

    @Transactional
    public User provisionStaff(String name, String email, String plainPassword, AppRole role, String mfaType) {
        return provisionStaff(name, email, plainPassword, role, mfaType, "MAIN");
    }

    @Transactional
    public User provisionStaff(String name, String email, String plainPassword, AppRole role, String mfaType, String branch) {
        if (repo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        if (role == null || role == AppRole.ROLE_CUSTOMER) {
            throw new IllegalArgumentException("Staff must be assigned an authorized staff role");
        }

        User staff = new User();
        staff.setName(name);
        staff.setEmail(email.trim().toLowerCase());
        staff.setPassword(passwordEncoder.encode(plainPassword));
        staff.setRole(role);
        staff.setUserType(UserType.STAFF);
        staff.setAccountNonLocked(true);
        staff.setFailedAttemptCount(0);
        staff.setBranch(role == AppRole.ROLE_ADMIN ? "ALL" : (branch != null ? branch : "MAIN"));

        // Set unenrolled initially so staff scans QR code during first login
        staff.setMfaSecret(null);
        staff.setMfaEnrolled(false);
        staff.setMfaType(mfaType != null ? mfaType : "TOTP");

        return repo.save(staff);
    }

    @Transactional
    public void updateStaffRole(Long staffId, AppRole newRole) {
        User staff = repo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff user not found: " + staffId));
        staff.setRole(newRole);
        repo.save(staff);
    }

    @Transactional
    public String resetMfaSecret(Long userId) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setMfaSecret(null);
        user.setMfaEnrolled(false);
        repo.save(user);
        return "MFA Reset - Enrollment required on next login";
    }
}
