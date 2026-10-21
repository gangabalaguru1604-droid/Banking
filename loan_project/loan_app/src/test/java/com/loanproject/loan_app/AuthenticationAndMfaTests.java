package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.AuthAuditLog;
import com.loanproject.loan_app.bean.MfaChallenge;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.repository.AuthAuditLogRepository;
import com.loanproject.loan_app.repository.MfaChallengeRepository;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.AesGcmEncryptionService;
import com.loanproject.loan_app.security.MfaService;
import com.loanproject.loan_app.security.MfaVerificationResult;
import com.loanproject.loan_app.security.TotpService;
import com.loanproject.loan_app.services.UserService;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationAndMfaTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MfaService mfaService;

    @Autowired
    private TotpService totpService;

    @Autowired
    private AesGcmEncryptionService encryptionService;

    @Autowired
    private MfaChallengeRepository challengeRepository;

    @Autowired
    private AuthAuditLogRepository auditRepo;

    @Autowired
    private com.loanproject.loan_app.repository.LoanRepository loanRepository;

    @Autowired
    private com.loanproject.loan_app.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private com.loanproject.loan_app.security.CustomerOtpDeliveryService customerOtpDeliveryService;

    private User testStaff;
    private String testSecret;

    @BeforeEach
    void setup() {
        mfaService.setStaffStaticOtpEnabled(true);
        mfaService.setStaffTestOtp("123456");
        customerOtpDeliveryService.setSimulateDeliveryFailure(false);
        testSecret = "JBSWY3DPEHPK3PXP";
        challengeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        loanRepository.deleteAll();
        userRepository.findByEmail("teststaff@bank.com").ifPresent(userRepository::delete);

        testStaff = new User();
        testStaff.setName("Test Staff");
        testStaff.setEmail("teststaff@bank.com");
        testStaff.setPassword(userService.provisionStaff("Test Staff", "teststaff@bank.com", "StaffPassword123!",
                AppRole.ROLE_LOAN_OFFICER, "TOTP").getPassword());
        testStaff = userRepository.findByEmail("teststaff@bank.com").orElseThrow();
        testStaff.setMfaSecret(encryptionService.encrypt(testSecret));
        testStaff.setMfaEnrolled(true);
        testStaff.setAccountNonLocked(true);
        testStaff.setFailedAttemptCount(0);
        userRepository.save(testStaff);
    }

    @Test
    @DisplayName("Test 1: Valid password followed by valid MFA grants access to protected routes")
    void testValidPasswordAndValidMfa() throws Exception {
        // Step 1: Submit valid password
        MvcResult stage1Result = mockMvc.perform(post("/login")
                .param("email", "teststaff@bank.com")
                .param("password", "StaffPassword123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-verify"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
        assertNotNull(session, "Session must be initialized for pending MFA");
        assertNotNull(session.getAttribute("PENDING_MFA_TOKEN"), "Challenge token must be in session");

        // Step 2: Generate valid TOTP code
        String validTotp = totpService.generateCurrentCode(testSecret);

        // Step 3: Submit valid MFA code
        mockMvc.perform(post("/mfa-verify")
                .session(session)
                .param("code", validTotp)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Step 4: Verify protected route is accessible with established session
        mockMvc.perform(get("/dashboard")
                .session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test 2: Valid password without MFA cannot access protected routes")
    void testValidPasswordWithoutMfaCannotAccessProtectedRoutes() throws Exception {
        // Step 1: Submit valid password
        MvcResult stage1Result = mockMvc.perform(post("/login")
                .param("email", "teststaff@bank.com")
                .param("password", "StaffPassword123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-verify"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);

        // Attempting to access protected routes (/dashboard, /applyLoan, etc.) must be redirected to /login
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/applyLoan").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Test 3: Invalid password produces generic error")
    void testInvalidPasswordProducesGenericError() throws Exception {
        mockMvc.perform(post("/login")
                .param("email", "teststaff@bank.com")
                .param("password", "WrongPassword!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));

        // Verify the login page renders the generic error message
        mockMvc.perform(get("/login?error=true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Invalid credentials or account unavailable."));
    }

    @Test
    @DisplayName("Test 4: Five consecutive failed password attempts lock the account")
    void testFiveFailedAttemptsLockAccount() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/login")
                    .param("email", "teststaff@bank.com")
                    .param("password", "WrongPassword" + i)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error=true"));
        }

        User lockedUser = userRepository.findByEmail("teststaff@bank.com").orElseThrow();
        assertFalse(lockedUser.isAccountNonLocked(), "Account must be locked after 5 failed attempts");
        assertEquals(5, lockedUser.getFailedAttemptCount());
        assertNotNull(lockedUser.getLockTime());
    }

    @Test
    @DisplayName("Test 5: Locked accounts cannot authenticate even with correct password")
    void testLockedAccountCannotAuthenticate() throws Exception {
        // Explicitly lock the account
        testStaff.setAccountNonLocked(false);
        userRepository.save(testStaff);

        // Attempt login with correct password
        mockMvc.perform(post("/login")
                .param("email", "teststaff@bank.com")
                .param("password", "StaffPassword123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("Test 7: Expired OTP is rejected")
    void testExpiredOtpIsRejected() {
        MfaChallenge challenge = mfaService.createChallenge(testStaff, "TOTP", null);
        // Force expiry in the past
        challenge.setExpiryTime(Instant.now().minusSeconds(10));
        challengeRepository.save(challenge);

        String validCode = totpService.generateCurrentCode(testSecret);
        MfaVerificationResult result = mfaService.verifyChallenge(challenge.getChallengeToken(), validCode, null);

        assertFalse(result.isSuccess());
        assertEquals("CHALLENGE_EXPIRED", result.getErrorCode());
    }

    @Test
    @DisplayName("Test 8: OTP cannot be reused")
    void testOtpCannotBeReused() {
        MfaChallenge challenge = mfaService.createChallenge(testStaff, "TOTP", null);
        String validCode = totpService.generateCurrentCode(testSecret);

        // First verification succeeds
        MfaVerificationResult result1 = mfaService.verifyChallenge(challenge.getChallengeToken(), validCode, null);
        assertTrue(result1.isSuccess());

        // Second verification with the same challenge must fail
        MfaVerificationResult result2 = mfaService.verifyChallenge(challenge.getChallengeToken(), validCode, null);
        assertFalse(result2.isSuccess());
        assertEquals("CHALLENGE_ALREADY_USED", result2.getErrorCode());
    }

    @Test
    @DisplayName("Test 9: Three incorrect OTP attempts lock the challenge")
    void testThreeIncorrectOtpAttemptsLockChallenge() {
        MfaChallenge challenge = mfaService.createChallenge(testStaff, "TOTP", null);

        // Attempt 1: Wrong code
        MfaVerificationResult r1 = mfaService.verifyChallenge(challenge.getChallengeToken(), "000000", null);
        assertFalse(r1.isSuccess());
        assertEquals(2, r1.getAttemptsRemaining());

        // Attempt 2: Wrong code
        MfaVerificationResult r2 = mfaService.verifyChallenge(challenge.getChallengeToken(), "111111", null);
        assertFalse(r2.isSuccess());
        assertEquals(1, r2.getAttemptsRemaining());

        // Attempt 3: Wrong code -> locks challenge
        MfaVerificationResult r3 = mfaService.verifyChallenge(challenge.getChallengeToken(), "222222", null);
        assertFalse(r3.isSuccess());
        assertEquals("CHALLENGE_LOCKED", r3.getErrorCode());

        MfaChallenge lockedChallenge = challengeRepository.findByChallengeToken(challenge.getChallengeToken()).orElseThrow();
        assertTrue(lockedChallenge.isLocked());
    }

    @Test
    @DisplayName("Test 10: Concurrent OTP verification cannot consume a challenge twice")
    void testConcurrentOtpVerificationCannotConsumeTwice() throws InterruptedException {
        MfaChallenge challenge = mfaService.createChallenge(testStaff, "TOTP", null);
        String validCode = totpService.generateCurrentCode(testSecret);

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    MfaVerificationResult res = mfaService.verifyChallenge(challenge.getChallengeToken(), validCode, null);
                    if (res.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Exactly one concurrent verification must succeed
        assertEquals(1, successCount.get(), "Exactly one concurrent verification must succeed");
        assertEquals(threads - 1, failureCount.get(), "All other concurrent attempts must fail");
    }

    @Test
    @DisplayName("Test 14: Logout invalidates the session")
    void testLogoutInvalidatesSession() throws Exception {
        // Authenticate
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", testStaff);

        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        assertTrue(session.isInvalid(), "Session must be invalidated on logout");
    }

    @Test
    @DisplayName("Test 15: CSRF protection works for state-changing forms")
    void testCsrfProtectionOnStateChangingForms() throws Exception {
        // POST /login without CSRF must be rejected with 403 Forbidden
        mockMvc.perform(post("/login")
                .param("email", "teststaff@bank.com")
                .param("password", "StaffPassword123!"))
                .andExpect(status().isForbidden());

        // POST /pay without CSRF must be rejected with 403 Forbidden
        mockMvc.perform(post("/pay")
                .param("loanId", "1")
                .param("amount", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test 17: Audit events are written without storing secrets")
    void testAuditEventsWrittenWithoutStoringSecrets() {
        long initialCount = auditRepo.count();

        // Perform audit event
        testStaff.setFailedAttemptCount(1);
        userRepository.save(testStaff);

        List<AuthAuditLog> logs = auditRepo.findAll();
        assertFalse(logs.isEmpty(), "Audit log records must exist");

        // Verify that NO audit log contains passwords or secrets
        for (AuthAuditLog l : logs) {
            if (l.getDetails() != null) {
                assertFalse(l.getDetails().contains("StaffPassword123!"), "Password must never appear in audit details");
                assertFalse(l.getDetails().contains(testSecret), "MFA Secret must never appear in audit details");
            }
        }
    }

    @Test
    @DisplayName("Test 18: Unenrolled user login redirects to /mfa-enroll with generated secret in session")
    void testFirstTimeLoginRedirectsToEnrollment() throws Exception {
        userRepository.findByEmail("newstaff@bank.com").ifPresent(userRepository::delete);
        userService.provisionStaff("New Staff", "newstaff@bank.com", "NewStaff123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");

        MvcResult result = mockMvc.perform(post("/login")
                .param("email", "newstaff@bank.com")
                .param("password", "NewStaff123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-enroll"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session, "Session must exist");
        assertNotNull(session.getAttribute("PENDING_MFA_ENROLL_SECRET"), "Pending enrollment secret must be generated");
        assertNotNull(session.getAttribute("PENDING_MFA_ENROLL_USER_ID"), "Pending enrollment user ID must exist");
        assertNotNull(session.getAttribute("PENDING_MFA_ENROLL_EXPIRY"), "Pending enrollment expiry must exist");

        String pendingSecret = (String) session.getAttribute("PENDING_MFA_ENROLL_SECRET");
        assertTrue(pendingSecret.length() >= 16, "Secret should be at least 16 Base32 characters");
    }

    @Test
    @DisplayName("Test 19: Protected /mfa-enroll page renders QR code and manual key for authenticated session")
    void testMfaEnrollPageRendersQrCode() throws Exception {
        userRepository.findByEmail("newstaff2@bank.com").ifPresent(userRepository::delete);
        User user = userService.provisionStaff("New Staff 2", "newstaff2@bank.com", "NewStaff123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");

        String rawSecret = totpService.generateSecret();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("PENDING_MFA_ENROLL_USER_ID", user.getId());
        session.setAttribute("PENDING_MFA_ENROLL_SECRET", rawSecret);
        session.setAttribute("PENDING_MFA_ENROLL_EXPIRY", Instant.now().plusSeconds(300));
        session.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", 0);

        mockMvc.perform(get("/mfa-enroll").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("qrCodeDataUri"))
                .andExpect(model().attributeExists("secretKey"))
                .andExpect(model().attribute("accountEmail", "newstaff2@bank.com"));
    }

    @Test
    @DisplayName("Test 20: First OTP verification enables MFA, encrypts secret in database, and establishes full authentication")
    void testSuccessfulFirstOtpVerificationEnablesMfa() throws Exception {
        userRepository.findByEmail("newstaff3@bank.com").ifPresent(userRepository::delete);
        User user = userService.provisionStaff("New Staff 3", "newstaff3@bank.com", "NewStaff123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");
        assertFalse(user.isMfaEnrolled(), "User must initially be unenrolled");

        String rawSecret = totpService.generateSecret();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("PENDING_MFA_ENROLL_USER_ID", user.getId());
        session.setAttribute("PENDING_MFA_ENROLL_SECRET", rawSecret);
        session.setAttribute("PENDING_MFA_ENROLL_EXPIRY", Instant.now().plusSeconds(300));
        session.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", 0);

        String validFirstOtp = totpService.generateCurrentCode(rawSecret);

        mockMvc.perform(post("/mfa-enroll")
                .session(session)
                .param("code", validFirstOtp)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updatedUser.isMfaEnrolled(), "MFA must now be enabled on user");
        assertNotNull(updatedUser.getMfaSecret(), "Encrypted secret must be saved");

        // Verify secret is encrypted with AES-GCM and decrypts to original
        String decrypted = encryptionService.decrypt(updatedUser.getMfaSecret());
        assertEquals(rawSecret, decrypted, "Decrypted secret must match raw secret");

        // Verify subsequent access to protected dashboard is authorized
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test 21: Invalid OTP during enrollment decrements remaining attempts and retains unenrolled status")
    void testInvalidOtpDuringEnrollment() throws Exception {
        userRepository.findByEmail("newstaff4@bank.com").ifPresent(userRepository::delete);
        User user = userService.provisionStaff("New Staff 4", "newstaff4@bank.com", "NewStaff123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");

        String rawSecret = totpService.generateSecret();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("PENDING_MFA_ENROLL_USER_ID", user.getId());
        session.setAttribute("PENDING_MFA_ENROLL_SECRET", rawSecret);
        session.setAttribute("PENDING_MFA_ENROLL_EXPIRY", Instant.now().plusSeconds(300));
        session.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", 0);

        mockMvc.perform(post("/mfa-enroll")
                .session(session)
                .param("code", "000000")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("attemptsRemaining", 2));

        User notUpdated = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(notUpdated.isMfaEnrolled(), "User must not be marked enrolled on invalid code");
    }

    @Test
    @DisplayName("Test 22: Three failed enrollment attempts locks enrollment and invalidates session")
    void testThreeFailedEnrollmentAttemptsLocks() throws Exception {
        userRepository.findByEmail("newstaff5@bank.com").ifPresent(userRepository::delete);
        User user = userService.provisionStaff("New Staff 5", "newstaff5@bank.com", "NewStaff123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");

        String rawSecret = totpService.generateSecret();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("PENDING_MFA_ENROLL_USER_ID", user.getId());
        session.setAttribute("PENDING_MFA_ENROLL_SECRET", rawSecret);
        session.setAttribute("PENDING_MFA_ENROLL_EXPIRY", Instant.now().plusSeconds(300));
        session.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", 2); // already 2 failed attempts

        mockMvc.perform(post("/mfa-enroll")
                .session(session)
                .param("code", "999999")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));

        assertTrue(session.isInvalid(), "Session must be invalidated after 3 failed attempts");
    }

    @Test
    @DisplayName("Test 23: Expired enrollment session is rejected")
    void testExpiredEnrollmentSessionRejected() throws Exception {
        userRepository.findByEmail("newstaff6@bank.com").ifPresent(userRepository::delete);
        User user = userService.provisionStaff("New Staff 6", "newstaff6@bank.com", "NewStaff123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");

        String rawSecret = totpService.generateSecret();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("PENDING_MFA_ENROLL_USER_ID", user.getId());
        session.setAttribute("PENDING_MFA_ENROLL_SECRET", rawSecret);
        session.setAttribute("PENDING_MFA_ENROLL_EXPIRY", Instant.now().minusSeconds(10)); // expired
        session.setAttribute("PENDING_MFA_ENROLL_ATTEMPTS", 0);

        mockMvc.perform(post("/mfa-enroll")
                .session(session)
                .param("code", totpService.generateCurrentCode(rawSecret))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired=true"));
    }

    @Test
    @DisplayName("Test 24: Unauthenticated access to /mfa-enroll is redirected to /login")
    void testUnauthenticatedAccessToMfaEnrollRedirected() throws Exception {
        mockMvc.perform(get("/mfa-enroll"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/mfa-enroll").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Test 25: Already enrolled account is not re-enrolled and goes to /mfa-verify")
    void testAlreadyEnrolledAccountContinuesToMfaVerify() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .param("email", "teststaff@bank.com")
                .param("password", "StaffPassword123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-verify"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session.getAttribute("PENDING_MFA_TOKEN"));
        assertNull(session.getAttribute("PENDING_MFA_ENROLL_SECRET"), "Should not generate new enrollment secret for enrolled user");
    }

    @Test
    @DisplayName("Test 26: Enrolled account with missing or corrupt secret is safely rejected with audit logging")
    void testEnrolledAccountWithInconsistentSecretSafelyHandled() throws Exception {
        userRepository.findByEmail("corrupt@bank.com").ifPresent(userRepository::delete);
        User user = userService.provisionStaff("Corrupt Staff", "corrupt@bank.com", "Corrupt123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");
        user.setMfaEnrolled(true);
        user.setMfaSecret("CORRUPT_INVALID_CIPHERTEXT");
        userRepository.save(user);

        // Should NOT bypass MFA, should NOT route to /mfa-verify or /dashboard, but redirect to /login?error=true
        mockMvc.perform(post("/login")
                .param("email", "corrupt@bank.com")
                .param("password", "Corrupt123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("Test 27: Account with mfaEnrolled=false but existing secret is safely routed to /mfa-enroll with diagnostic log")
    void testUnconfirmedEnrollmentRoutesToEnroll() throws Exception {
        userRepository.findByEmail("unconfirmed@bank.com").ifPresent(userRepository::delete);
        User user = userService.provisionStaff("Unconfirmed Staff", "unconfirmed@bank.com", "Unconfirmed123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");
        user.setMfaEnrolled(false);
        user.setMfaSecret("STALE_SECRET");
        userRepository.save(user);

        mockMvc.perform(post("/login")
                .param("email", "unconfirmed@bank.com")
                .param("password", "Unconfirmed123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-enroll"));
    }

    @Test
    @DisplayName("Test 28: Complete cycle: enrollment, logout, and subsequent login routes to /mfa-verify")
    void testFullEnrollmentThenLogoutThenSubsequentLogin() throws Exception {
        userRepository.findByEmail("lifecycle@bank.com").ifPresent(userRepository::delete);
        User user = userService.provisionStaff("Lifecycle Staff", "lifecycle@bank.com", "LifeCycle123!", AppRole.ROLE_LOAN_OFFICER, "TOTP");

        // 1. Initial login -> redirects to /mfa-enroll
        MvcResult r1 = mockMvc.perform(post("/login")
                .param("email", "lifecycle@bank.com")
                .param("password", "LifeCycle123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-enroll"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) r1.getRequest().getSession(false);
        String secret = (String) session.getAttribute("PENDING_MFA_ENROLL_SECRET");
        assertNotNull(secret);

        // 2. Submit initial valid code -> enables MFA and grants dashboard
        String initialCode = totpService.generateCurrentCode(secret);
        mockMvc.perform(post("/mfa-enroll")
                .session(session)
                .param("code", initialCode)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        User enrolledUser = userRepository.findByEmail("lifecycle@bank.com").orElseThrow();
        assertTrue(enrolledUser.isMfaEnrolled(), "User must be enrolled now");

        // 3. Logout
        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));

        // 4. Subsequent login -> MUST redirect to /mfa-verify, NOT /mfa-enroll!
        MvcResult r2 = mockMvc.perform(post("/login")
                .param("email", "lifecycle@bank.com")
                .param("password", "LifeCycle123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-verify"))
                .andReturn();

        MockHttpSession session2 = (MockHttpSession) r2.getRequest().getSession(false);
        assertNotNull(session2.getAttribute("PENDING_MFA_TOKEN"));
        assertNull(session2.getAttribute("PENDING_MFA_ENROLL_SECRET"));

        // 5. Subsequent valid MFA code grants dashboard
        String secondCode = totpService.generateCurrentCode(secret);
        mockMvc.perform(post("/mfa-verify")
                .session(session2)
                .param("code", secondCode)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Test: Admin login and MFA verification redirects to Admin Dashboard (/dashboard), not /admin/staff")
    void testAdminLoginAndMfaRedirectsToDashboard() throws Exception {
        userRepository.findByEmail("admin.redirect@bank.com").ifPresent(userRepository::delete);

        User admin = new User();
        admin.setName("Admin Redirect Test");
        admin.setEmail("admin.redirect@bank.com");
        admin.setPassword(userService.provisionStaff("Admin Redirect Test", "admin.redirect@bank.com", "AdminPass123!",
                AppRole.ROLE_ADMIN, "TOTP").getPassword());
        admin = userRepository.findByEmail("admin.redirect@bank.com").orElseThrow();
        admin.setMfaSecret(encryptionService.encrypt(testSecret));
        admin.setMfaEnrolled(true);
        admin.setAccountNonLocked(true);
        admin.setFailedAttemptCount(0);
        userRepository.save(admin);

        // Stage 1: Password login
        MvcResult stage1Result = mockMvc.perform(post("/login")
                .param("email", "admin.redirect@bank.com")
                .param("password", "AdminPass123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-verify"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
        assertNotNull(session);

        // Stage 2: MFA verification -> MUST redirect to /dashboard, NOT /admin/staff
        String validTotp = totpService.generateCurrentCode(testSecret);
        mockMvc.perform(post("/mfa-verify")
                .session(session)
                .param("code", validTotp)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Stage 3: Admin Dashboard is accessible
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        // Stage 4: Staff Management is still accessible manually through navigation
        mockMvc.perform(get("/admin/staff").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-staff"));

        challengeRepository.deleteAll();
        userRepository.delete(admin);
    }

    @Test
    @DisplayName("Test: Customer login and MFA verification redirects to Customer Dashboard (/dashboard)")
    void testCustomerLoginAndMfaRedirectsToDashboard() throws Exception {
        challengeRepository.deleteAll();
        userRepository.findByEmail("customer.redirect@bank.com").ifPresent(userRepository::delete);

        User customer = new User("Customer Test", "customer.redirect@bank.com", "CustPass123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        customer.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("CustPass123!"));
        customer.setMfaSecret(encryptionService.encrypt(testSecret));
        customer.setMfaEnrolled(true);
        customer.setMfaType("DYNAMIC_EMAIL");
        customer = userRepository.save(customer);

        // Stage 1: Password login
        MvcResult stage1Result = mockMvc.perform(post("/login")
                .param("email", "customer.redirect@bank.com")
                .param("password", "CustPass123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-verify"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
        assertNotNull(session);

        // Stage 2: MFA verification -> redirects to /dashboard
        String customerOtp = customerOtpDeliveryService.getLastDeliveredOtpForTesting("customer.redirect@bank.com");
        assertNotNull(customerOtp);
        mockMvc.perform(post("/mfa-verify")
                .session(session)
                .param("code", customerOtp)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        // Stage 3: Customer Dashboard is accessible
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        // Stage 4: Customer cannot access /admin/staff
        mockMvc.perform(get("/admin/staff").session(session))
                .andExpect(status().isForbidden());

        challengeRepository.deleteAll();
        userRepository.delete(customer);
    }

    @Test
    @DisplayName("Test: Unsuccessful MFA attempt never redirects to protected dashboard")
    void testUnsuccessfulMfaNeverRedirectsToDashboard() throws Exception {
        // Stage 1: Password login for staff
        MvcResult stage1Result = mockMvc.perform(post("/login")
                .param("email", "teststaff@bank.com")
                .param("password", "StaffPassword123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-verify"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
        assertNotNull(session);

        // Stage 2: Invalid MFA code submission
        mockMvc.perform(post("/mfa-verify")
                .session(session)
                .param("code", "000000")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("mfa"))
                .andExpect(model().attributeExists("errorMessage"));

        // Stage 3: Access to dashboard must still be blocked (redirected to /login)
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Test: Staff login succeeds using valid credentials followed by configured static test OTP")
    void testStaffLoginWithConfiguredStaticTestOtp() throws Exception {
        MvcResult stage1Result = mockMvc.perform(post("/login")
                .param("email", "teststaff@bank.com")
                .param("password", "StaffPassword123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa-verify"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
        assertNotNull(session);

        // Submit static test OTP "123456"
        mockMvc.perform(post("/mfa-verify")
                .session(session)
                .param("code", "123456")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Test: Staff login static OTP is configurable via environment / service configuration")
    void testStaffLoginConfigurableStaticOtp() throws Exception {
        String originalOtp = mfaService.getStaffTestOtp();
        try {
            mfaService.setStaffTestOtp("654321");

            MvcResult stage1Result = mockMvc.perform(post("/login")
                    .param("email", "teststaff@bank.com")
                    .param("password", "StaffPassword123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-verify"))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
            assertNotNull(session);

            // Old OTP "123456" is rejected
            mockMvc.perform(post("/mfa-verify")
                    .session(session)
                    .param("code", "123456")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("mfa"))
                    .andExpect(model().attributeExists("errorMessage"));

            // New configured OTP "654321" succeeds
            mockMvc.perform(post("/mfa-verify")
                    .session(session)
                    .param("code", "654321")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
        } finally {
            mfaService.setStaffTestOtp(originalOtp);
        }
    }

    @Test
    @DisplayName("Test: Production configuration disables static staff OTP and requires proper MFA")
    void testProductionConfigurationDisablesStaticStaffOtp() throws Exception {
        boolean originalEnabled = mfaService.isStaffStaticOtpEnabled();
        try {
            mfaService.setStaffStaticOtpEnabled(false);

            MvcResult stage1Result = mockMvc.perform(post("/login")
                    .param("email", "teststaff@bank.com")
                    .param("password", "StaffPassword123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-verify"))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
            assertNotNull(session);

            // Static OTP "123456" is rejected when static OTP is disabled
            mockMvc.perform(post("/mfa-verify")
                    .session(session)
                    .param("code", "123456")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("mfa"))
                    .andExpect(model().attributeExists("errorMessage"));

            // Enrolled TOTP code succeeds
            String validTotp = totpService.generateCurrentCode(testSecret);
            mockMvc.perform(post("/mfa-verify")
                    .session(session)
                    .param("code", validTotp)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
        } finally {
            mfaService.setStaffStaticOtpEnabled(originalEnabled);
        }
    }

    @Test
    @DisplayName("Test: Customer login completes dynamic MFA with fresh time-limited OTP")
    void testCustomerLoginWithDynamicMfa() throws Exception {
        userRepository.findByEmail("customer.dynamic@bank.com").ifPresent(userRepository::delete);
        User customer = new User("Dynamic Customer", "customer.dynamic@bank.com", "CustomerPass123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userService.register(customer);
        customer = userRepository.findByEmail("customer.dynamic@bank.com").orElseThrow();
        customer.setMfaType("DYNAMIC_EMAIL");
        customer.setMfaEnrolled(true);
        userRepository.save(customer);

        try {
            // Stage 1: Primary credentials
            MvcResult stage1Result = mockMvc.perform(post("/login")
                    .param("email", "customer.dynamic@bank.com")
                    .param("password", "CustomerPass123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-verify"))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
            assertNotNull(session);

            String challengeToken = (String) session.getAttribute("PENDING_MFA_TOKEN");
            assertNotNull(challengeToken);

            MfaChallenge challenge = challengeRepository.findByChallengeToken(challengeToken).orElseThrow();
            assertEquals("DYNAMIC_EMAIL", challenge.getMfaType());
            assertNotNull(challenge.getHashedOtpCode(), "Dynamic OTP must be stored as hash");

            // Retrieve delivered dynamic OTP via testing hook (no logs leaked)
            String deliveredOtp = customerOtpDeliveryService.getLastDeliveredOtpForTesting("customer.dynamic@bank.com");
            assertNotNull(deliveredOtp);
            assertEquals(6, deliveredOtp.length());

            // Stage 2: Submit dynamic OTP
            mockMvc.perform(post("/mfa-verify")
                    .session(session)
                    .param("code", deliveredOtp)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));

            mockMvc.perform(get("/dashboard").session(session))
                    .andExpect(status().isOk());
        } finally {
            userRepository.findByEmail("customer.dynamic@bank.com").ifPresent(userRepository::delete);
        }
    }

    @Test
    @DisplayName("Test: Static staff OTP is strictly rejected for customer accounts")
    void testStaticStaffOtpRejectedForCustomerAccounts() throws Exception {
        userRepository.findByEmail("customer.strict@bank.com").ifPresent(userRepository::delete);
        User customer = new User("Strict Customer", "customer.strict@bank.com", "CustomerPass123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userService.register(customer);
        customer = userRepository.findByEmail("customer.strict@bank.com").orElseThrow();
        customer.setMfaType("DYNAMIC_EMAIL");
        customer.setMfaEnrolled(true);
        userRepository.save(customer);

        try {
            MvcResult stage1Result = mockMvc.perform(post("/login")
                    .param("email", "customer.strict@bank.com")
                    .param("password", "CustomerPass123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-verify"))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
            assertNotNull(session);

            // Attempting to use the staff static OTP "123456" must be REJECTED
            mockMvc.perform(post("/mfa-verify")
                    .session(session)
                    .param("code", "123456")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("mfa"))
                    .andExpect(model().attribute("errorMessage", "Invalid authentication code. 2 attempts remaining."));
        } finally {
            userRepository.findByEmail("customer.strict@bank.com").ifPresent(userRepository::delete);
        }
    }

    @Test
    @DisplayName("Test: Customer dynamic MFA locks after 3 incorrect attempts")
    void testCustomerMfaLocksAfterThreeIncorrectAttempts() throws Exception {
        userRepository.findByEmail("customer.lock@bank.com").ifPresent(userRepository::delete);
        User customer = new User("Lock Customer", "customer.lock@bank.com", "CustomerPass123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userService.register(customer);
        customer = userRepository.findByEmail("customer.lock@bank.com").orElseThrow();
        customer.setMfaType("DYNAMIC_EMAIL");
        customer.setMfaEnrolled(true);
        userRepository.save(customer);

        try {
            MvcResult stage1Result = mockMvc.perform(post("/login")
                    .param("email", "customer.lock@bank.com")
                    .param("password", "CustomerPass123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-verify"))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
            assertNotNull(session);
            String challengeToken = (String) session.getAttribute("PENDING_MFA_TOKEN");

            // Attempt 1: Wrong code
            mockMvc.perform(post("/mfa-verify").session(session).param("code", "000000").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("attemptsRemaining", 2));

            // Attempt 2: Wrong code
            mockMvc.perform(post("/mfa-verify").session(session).param("code", "111111").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("attemptsRemaining", 1));

            // Attempt 3: Wrong code -> locks challenge and redirects to login with error
            mockMvc.perform(post("/mfa-verify").session(session).param("code", "222222").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error=true"));

            MfaChallenge lockedChallenge = challengeRepository.findByChallengeToken(challengeToken).orElseThrow();
            assertTrue(lockedChallenge.isLocked());
        } finally {
            userRepository.findByEmail("customer.lock@bank.com").ifPresent(userRepository::delete);
        }
    }

    @Test
    @DisplayName("Test: Customer MFA delivery failure does not bypass MFA and allows safe retry")
    void testCustomerMfaDeliveryFailureDoesNotBypassMfa() throws Exception {
        userRepository.findByEmail("customer.fail@bank.com").ifPresent(userRepository::delete);
        User customer = new User("Fail Customer", "customer.fail@bank.com", "CustomerPass123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userService.register(customer);
        customer = userRepository.findByEmail("customer.fail@bank.com").orElseThrow();
        customer.setMfaType("DYNAMIC_EMAIL");
        customer.setMfaEnrolled(true);
        userRepository.save(customer);

        try {
            customerOtpDeliveryService.setSimulateDeliveryFailure(true);

            // Attempt login when delivery fails
            mockMvc.perform(post("/login")
                    .param("email", "customer.fail@bank.com")
                    .param("password", "CustomerPass123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?mfaDeliveryError=true"));

            // Verify login page renders clear error message
            mockMvc.perform(get("/login?mfaDeliveryError=true"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("errorMessage", "Unable to deliver dynamic authentication code. Please try logging in again or contact customer support."));

            // Unauthenticated user CANNOT access protected dashboard
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        } finally {
            customerOtpDeliveryService.setSimulateDeliveryFailure(false);
            userRepository.findByEmail("customer.fail@bank.com").ifPresent(userRepository::delete);
        }
    }

    @Test
    @DisplayName("Test: Customer login with TOTP Authenticator QR code scan and verification")
    void testCustomerLoginWithTotpAuthenticatorQrScanEnrollment() throws Exception {
        userRepository.findByEmail("customer.totp@bank.com").ifPresent(userRepository::delete);
        User customer = new User("TOTP Customer", "customer.totp@bank.com", "CustomerPass123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userService.register(customer);
        // Initially unenrolled (mfaEnrolled = false, mfaType = "TOTP")

        try {
            // Stage 1: Customer logs in
            MvcResult loginResult = mockMvc.perform(post("/login")
                    .param("email", "customer.totp@bank.com")
                    .param("password", "CustomerPass123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-enroll"))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
            assertNotNull(session);
            String enrollSecret = (String) session.getAttribute("PENDING_MFA_ENROLL_SECRET");
            assertNotNull(enrollSecret);

            // Verify /mfa-enroll displays QR code
            mockMvc.perform(get("/mfa-enroll").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("mfa-enroll"))
                    .andExpect(model().attributeExists("qrCodeDataUri"))
                    .andExpect(model().attributeExists("secretKey"));

            // Submit 6-digit code generated from Authenticator secret
            String totpCode = totpService.generateCurrentCode(enrollSecret);
            mockMvc.perform(post("/mfa-enroll")
                    .session(session)
                    .param("code", totpCode)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));

            // Verify customer is now enrolled in database
            User enrolledCustomer = userRepository.findByEmail("customer.totp@bank.com").orElseThrow();
            assertTrue(enrolledCustomer.isMfaEnrolled());
            assertEquals("TOTP", enrolledCustomer.getMfaType());

            // Subsequent login routes to /mfa-verify
            MvcResult secondLogin = mockMvc.perform(post("/login")
                    .param("email", "customer.totp@bank.com")
                    .param("password", "CustomerPass123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-verify"))
                    .andReturn();

            MockHttpSession secondSession = (MockHttpSession) secondLogin.getRequest().getSession(false);
            assertNotNull(secondSession);

            // Submit current Authenticator code on /mfa-verify
            String currentTotp = totpService.generateCurrentCode(enrollSecret);
            mockMvc.perform(post("/mfa-verify")
                    .session(secondSession)
                    .param("code", currentTotp)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));

        } finally {
            userRepository.findByEmail("customer.totp@bank.com").ifPresent(userRepository::delete);
        }
    }

    @Test
    @DisplayName("Test: Customer can request resend of dynamic verification code")
    void testCustomerMfaResendCode() throws Exception {
        userRepository.findByEmail("customer.resend@bank.com").ifPresent(userRepository::delete);
        User customer = new User("Resend Customer", "customer.resend@bank.com", "CustomerPass123!",
                AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        userService.register(customer);
        customer = userRepository.findByEmail("customer.resend@bank.com").orElseThrow();
        customer.setMfaType("DYNAMIC_EMAIL");
        customer.setMfaEnrolled(true);
        userRepository.save(customer);

        try {
            MvcResult stage1Result = mockMvc.perform(post("/login")
                    .param("email", "customer.resend@bank.com")
                    .param("password", "CustomerPass123!")
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-verify"))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) stage1Result.getRequest().getSession(false);
            assertNotNull(session);
            String firstOtp = customerOtpDeliveryService.getLastDeliveredOtpForTesting("customer.resend@bank.com");

            // Wait 5 seconds to pass rate limit
            Thread.sleep(5100);

            // Perform resend
            mockMvc.perform(post("/mfa-resend")
                    .session(session)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/mfa-verify"));

            String secondOtp = customerOtpDeliveryService.getLastDeliveredOtpForTesting("customer.resend@bank.com");
            assertNotNull(secondOtp);

            // Verify new code can authenticate
            mockMvc.perform(post("/mfa-verify")
                    .session(session)
                    .param("code", secondOtp)
                    .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
        } finally {
            userRepository.findByEmail("customer.resend@bank.com").ifPresent(userRepository::delete);
        }
    }
}
