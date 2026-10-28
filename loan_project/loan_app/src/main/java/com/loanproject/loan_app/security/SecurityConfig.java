package com.loanproject.loan_app.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final TwoFactorAuthenticationSuccessHandler twoFactorSuccessHandler;
    private final CustomAuthenticationFailureHandler failureHandler;
    private final CustomLogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          TwoFactorAuthenticationSuccessHandler twoFactorSuccessHandler,
                          CustomAuthenticationFailureHandler failureHandler,
                          CustomLogoutSuccessHandler logoutSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.twoFactorSuccessHandler = twoFactorSuccessHandler;
        this.failureHandler = failureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(true); // Return generic error
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/webhooks/**"))
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Allow internal view forwarding and error dispatching for JSP views
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers("/WEB-INF/**").permitAll()

                // Public routes
                .requestMatchers(
                    "/",
                    "/jsp-home",
                    "/index.html",
                    "/assets/**",
                    "/api/auth/**",
                    "/login",
                    "/register",
                    "/forgot-password",
                    "/reset-password",
                    "/mfa-verify",
                    "/mfa-enroll",
                    "/session/keep-alive",
                    "/health",
                    "/ping",
                    "/api/webhooks/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/jakarta.faces.resource/**",
                    "/error"
                ).permitAll()

                // Admin-only management endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Loan origination: Loan Officers, Admin, or Customers applying for self
                .requestMatchers("/applyLoan").hasAnyRole("CUSTOMER", "LOAN_OFFICER", "ADMIN")

                // Repayment processing: Strictly restricted to Customers paying self (All staff roles are restricted)
                .requestMatchers("/payPage", "/pay").hasRole("CUSTOMER")

                // Staff Loan Processing (Approve / Reject): Loan Officers, Admins (both REST/form and JSF)
                .requestMatchers(
                    "/staff/loans/approve",
                    "/staff/loans/reject",
                    "/loan-approval.xhtml",
                    "/loan-approval.jsf",
                    "/loan-approval",
                    "/loan-approval/**"
                ).hasAnyRole("LOAN_OFFICER", "ADMIN")

                // Staff Collections Recording: Collections Agents, Collections Managers, Admins
                .requestMatchers("/staff/collections/record").hasAnyRole("COLLECTIONS_AGENT", "COLLECTIONS_MANAGER", "ADMIN")

                // Loan search, customer profile, and 360 degree details: Loan Officers, Collections staff, Admins
                .requestMatchers("/loan-search", "/loan-search/**", "/customer-profile", "/customer-profile/**", "/loan-details", "/loan-details/**").hasAnyRole("LOAN_OFFICER", "COLLECTIONS_AGENT", "COLLECTIONS_MANAGER", "ADMIN")

                // Dashboard
                .requestMatchers("/dashboard").authenticated()

                // JSF Settings - Admin Only (Role & Permissions, Audit Settings, System Settings)
                .requestMatchers(
                    "/role-permissions.xhtml", "/role-permissions.jsf", "/role-permissions",
                    "/audit-settings.xhtml", "/audit-settings.jsf", "/audit-settings",
                    "/system-settings.xhtml", "/system-settings.jsf", "/system-settings"
                ).hasRole("ADMIN")

                // JSF Settings - Loan Underwriting Policy (Loan Officers and Admin)
                .requestMatchers(
                    "/loan-policy-settings.xhtml", "/loan-policy-settings.jsf", "/loan-policy-settings"
                ).hasAnyRole("LOAN_OFFICER", "ADMIN")

                // JSF Settings - All Authenticated Users (Profile, Security, Notifications, Hub)
                .requestMatchers(
                    "/profile-settings.xhtml", "/profile-settings.jsf", "/profile-settings",
                    "/security-settings.xhtml", "/security-settings.jsf", "/security-settings",
                    "/notification-settings.xhtml", "/notification-settings.jsf", "/notification-settings",
                    "/settings-hub.xhtml", "/settings-hub.jsf", "/settings-hub"
                ).authenticated()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(twoFactorSuccessHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .clearAuthentication(true)
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
            )
            .headers(headers -> headers
                .cacheControl(org.springframework.security.config.Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline';")
                )
            );

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://*.onrender.com",
            "http://*.onrender.com"
        ));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setExposedHeaders(java.util.List.of("X-CSRF-TOKEN", "Set-Cookie"));
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
