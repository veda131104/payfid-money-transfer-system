package com.company.mts.service;

import com.company.mts.dto.ResetPasswordRequest;
import java.time.LocalDateTime;
import java.util.UUID;

import com.company.mts.dto.LoginRequest;
import com.company.mts.dto.SignupRequest;
import com.company.mts.dto.CredentialsResponse;
import com.company.mts.entity.AuthUser;
import com.company.mts.exception.DuplicateUserException;
import com.company.mts.exception.InvalidCredentialsException;
import com.company.mts.repository.AuthUserRepository;
import org.springframework.stereotype.Service;
import com.company.mts.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Autowired
    public AuthService(AuthUserRepository authUserRepository,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.authUserRepository = authUserRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        log.info("[AuthService] Initialized with all dependencies");
    }

    public AuthUser signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String name = request.getName().trim();
        log.info("[AuthService] signup: Processing signup for name='{}', email='{}'", name, email);

        boolean emailExists = authUserRepository.existsByEmailIgnoreCase(email);
        boolean nameExists = authUserRepository.existsByNameIgnoreCase(name);
        log.debug("[AuthService] signup: emailExists={}, nameExists={}", emailExists, nameExists);

        if (emailExists || nameExists) {
            log.warn("[AuthService] signup: DUPLICATE detected. emailExists={}, nameExists={}", emailExists, nameExists);
            throw new DuplicateUserException("Email or username already in use");
        }

        AuthUser user = new AuthUser();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setFirstLogin(true);

        log.info("[AuthService] signup: Saving new user to database: name='{}', email='{}'", name, email);
        AuthUser savedUser = authUserRepository.save(user);
        log.info("[AuthService] signup: User SAVED successfully. id={}, name='{}', email='{}'",
                savedUser.getId(), savedUser.getName(), savedUser.getEmail());
        return savedUser;
    }

    public AuthUser login(LoginRequest request) {
        String name = request.getName().trim();
        log.info("[AuthService] login: Processing login for name='{}'", name);

        AuthUser user = authUserRepository
                .findByNameIgnoreCase(name)
                .orElseThrow(() -> {
                    log.warn("[AuthService] login: User NOT FOUND with name='{}'", name);
                    return new InvalidCredentialsException("Invalid username or password");
                });

        log.debug("[AuthService] login: User found in DB. id={}, name='{}'", user.getId(), user.getName());

        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPassword())) {
            log.warn("[AuthService] login: Password MISMATCH for name='{}'", name);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        log.info("[AuthService] login: Password verified for name='{}'", name);

        if (request.isRememberMe()) {
            String token = UUID.randomUUID().toString();
            user.setRememberToken(token);
            user.setRememberTokenExpiry(LocalDateTime.now().plusDays(30));
            authUserRepository.save(user);
            log.info("[AuthService] login: Remember-me token generated for name='{}'", name);
        }

        // generate and persist JWT token (JwtTokenProvider saves token to DB)
        tokenProvider.generateToken(user);
        log.info("[AuthService] login: JWT token generated for name='{}'. Login SUCCESS", name);

        return user;
    }

    public CredentialsResponse getCredentialsByToken(String token) {
        log.info("[AuthService] getCredentialsByToken: Looking up user by remember token");
        AuthUser user = authUserRepository.findByRememberToken(token)
                .orElseThrow(() -> {
                    log.warn("[AuthService] getCredentialsByToken: Invalid remember token");
                    return new RuntimeException("Invalid remember token");
                });

        if (user.getRememberTokenExpiry() == null
                || user.getRememberTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("[AuthService] getCredentialsByToken: Remember token EXPIRED for name='{}'", user.getName());
            throw new RuntimeException("Remember token expired");
        }

        log.info("[AuthService] getCredentialsByToken: Credentials retrieved for name='{}'", user.getName());
        return new CredentialsResponse(user.getName(), user.getPassword());
    }

    public void processForgotPassword(String username) {
        log.info("[AuthService] processForgotPassword: Processing forgot password request for user: {}", username);
        authUserRepository.findByNameIgnoreCase(username.trim()).ifPresent(user -> {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                String token = UUID.randomUUID().toString();
                user.setRecoveryToken(token);
                user.setRecoveryTokenExpiry(LocalDateTime.now().plusMinutes(15));
                authUserRepository.save(user);

                log.info("[AuthService] processForgotPassword: Recovery token generated. Sending recovery link to: {}", user.getEmail());
                try {
                    emailService.sendForgotPasswordEmail(user.getEmail(), user.getName(), token);
                    log.info("[AuthService] processForgotPassword: Recovery email sent successfully to: {}", user.getEmail());
                } catch (Exception e) {
                    log.error("[AuthService] processForgotPassword: Failed to send recovery email to '{}': {} - {}",
                            user.getEmail(), e.getClass().getSimpleName(), e.getMessage());
                }
            } else {
                log.warn("[AuthService] processForgotPassword: User {} has no email associated", username);
            }
        });
    }

    public AuthUser loginWithToken(String token) {
        log.info("[AuthService] loginWithToken: Verifying magic link token");
        AuthUser user = authUserRepository.findByRecoveryToken(token)
                .orElseThrow(() -> {
                    log.warn("[AuthService] loginWithToken: Invalid recovery link token");
                    return new IllegalArgumentException("Invalid recovery link");
                });

        if (user.getRecoveryTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("[AuthService] loginWithToken: Recovery link EXPIRED for user '{}'", user.getName());
            throw new IllegalArgumentException("Recovery link has expired");
        }

        // Clear token after use for security
        user.setRecoveryToken(null);
        user.setRecoveryTokenExpiry(null);
        authUserRepository.save(user);

        log.info("[AuthService] loginWithToken: User '{}' logged in successfully via magic link", user.getName());
        return user;
    }

    public AuthUser resetPassword(ResetPasswordRequest request) {
        log.info("[AuthService] resetPassword: Resetting password for recovery token");
        AuthUser user = authUserRepository.findByRecoveryToken(request.getToken())
                .orElseThrow(() -> {
                    log.warn("[AuthService] resetPassword: Invalid reset link token");
                    return new IllegalArgumentException("Invalid reset link");
                });

        if (user.getRecoveryTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("[AuthService] resetPassword: Reset link EXPIRED for user '{}'", user.getName());
            throw new IllegalArgumentException("Reset link has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRecoveryToken(null);
        user.setRecoveryTokenExpiry(null);

        authUserRepository.save(user);
        log.info("[AuthService] resetPassword: Password reset successfully for user: {}", user.getName());
        return user;
    }
}
