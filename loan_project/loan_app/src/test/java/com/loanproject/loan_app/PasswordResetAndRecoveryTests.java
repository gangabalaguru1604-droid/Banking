package com.loanproject.loan_app;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.loanproject.loan_app.bean.AppRole;
import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.repository.UserRepository;
import com.loanproject.loan_app.security.PasswordResetService;

@SpringBootTest
@AutoConfigureMockMvc
public class PasswordResetAndRecoveryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.loanproject.loan_app.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    private User existingUser;

    @BeforeEach
    void setup() {
        passwordResetTokenRepository.deleteAll();
        userRepository.findByEmail("recovery.user@bank.com").ifPresent(userRepository::delete);

        existingUser = new User("Recovery User", "recovery.user@bank.com",
                passwordEncoder.encode("InitialPassword123!"), AppRole.ROLE_CUSTOMER, UserType.CUSTOMER);
        existingUser = userRepository.save(existingUser);
    }

    @Test
    @DisplayName("Test 16: Password-reset responses do not reveal whether an account exists")
    void testPasswordResetDoesNotRevealAccountExistence() throws Exception {
        // Request for existing email
        MvcResult existingResult = mockMvc.perform(post("/forgot-password")
                .param("email", "recovery.user@bank.com")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        String existingResponseMsg = (String) existingResult.getModelAndView().getModel().get("infoMessage");
        assertNotNull(existingResponseMsg);

        // Request for non-existing email
        MvcResult nonExistingResult = mockMvc.perform(post("/forgot-password")
                .param("email", "doesnotexist999@bank.com")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        String nonExistingResponseMsg = (String) nonExistingResult.getModelAndView().getModel().get("infoMessage");
        assertNotNull(nonExistingResponseMsg);

        // Both messages must be completely identical
        assertEquals(existingResponseMsg, nonExistingResponseMsg,
                "Password reset messages must be identical for existing and non-existing accounts to prevent enumeration");

        // Verify valid reset token allows password reset
        String token = passwordResetService.getLastGeneratedTokenForTesting();
        assertNotNull(token, "Reset token must be generated for existing account");

        // Use token to reset password
        mockMvc.perform(post("/reset-password")
                .param("token", token)
                .param("password", "NewSecurePassword123!")
                .param("confirmPassword", "NewSecurePassword123!")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?resetSuccess=true"));

        // Verify new password is set
        User updated = userRepository.findByEmail("recovery.user@bank.com").orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecurePassword123!", updated.getPassword()));

        // Token cannot be reused
        assertFalse(passwordResetService.validateToken(token), "Reset token must be single-use and invalid after consumption");
    }
}
