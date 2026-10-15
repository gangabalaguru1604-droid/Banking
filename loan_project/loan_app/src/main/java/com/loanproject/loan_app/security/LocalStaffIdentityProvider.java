package com.loanproject.loan_app.security;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.loanproject.loan_app.bean.User;
import com.loanproject.loan_app.bean.UserType;
import com.loanproject.loan_app.repository.UserRepository;

@Component
public class LocalStaffIdentityProvider implements BankIdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalStaffIdentityProvider.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bank.idp.production-enabled:false}")
    private boolean productionConfigured;

    public LocalStaffIdentityProvider(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean isProductionProviderConfigured() {
        return productionConfigured;
    }

    @Override
    public String getProviderName() {
        return productionConfigured ? "Enterprise LDAP/OIDC Bank IdP" : "Development Local Staff Identity Store";
    }

    @Override
    public Optional<User> authenticateStaff(String staffIdOrEmail, String rawPassword) {
        Optional<User> optionalUser = userRepository.findByEmail(staffIdOrEmail);
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        User user = optionalUser.get();

        // Enforce: Do not accept customer accounts as bank staff accounts
        if (user.getUserType() != UserType.STAFF) {
            log.warn("Account {} is a customer account, rejected from staff authentication", staffIdOrEmail);
            return Optional.empty();
        }

        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return Optional.of(user);
        }

        return Optional.empty();
    }
}
