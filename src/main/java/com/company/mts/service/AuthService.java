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
    }

    public AuthUser signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String name = request.getName().trim();
        if (authUserRepository.existsByEmailIgnoreCase(email) || authUserRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateUserException("Email or username already in use");
        }

        AuthUser user = new AuthUser();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));

        log.info("Saving new user to database: email={}", email);
        return authUserRepository.save(user);
    }

    public AuthUser login(LoginRequest request) {
        AuthUser user = authUserRepository
                .findByNameIgnoreCase(request.getName().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (request.isRememberMe()) {
            String token = UUID.randomUUID().toString();
            user.setRememberToken(token);
            user.setRememberTokenExpiry(LocalDateTime.now().plusDays(30));
            authUserRepository.save(user);
        }

        // generate and persist JWT token (JwtTokenProvider saves token to DB)
        tokenProvider.generateToken(user);

        return user;
    }

    public CredentialsResponse getCredentialsByToken(String token) {
        AuthUser user = authUserRepository.findByRememberToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid remember token"));

        if (user.getRememberTokenExpiry() == null
                || user.getRememberTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Remember token expired");
        }

        return new CredentialsResponse(user.getName(), user.getPassword());
    }

    public void processForgotPassword(String username) {
        log.info("Processing forgot password request for user: {}", username);
        authUserRepository.findByNameIgnoreCase(username.trim()).ifPresent(user -> {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                String token = UUID.randomUUID().toString();
                user.setRecoveryToken(token);
                user.setRecoveryTokenExpiry(LocalDateTime.now().plusMinutes(15));
                authUserRepository.save(user);

                log.info("Sending recovery link to: {}", user.getEmail());
                emailService.sendForgotPasswordEmail(user.getEmail(), user.getName(), token);
            } else {
                log.warn("User {} has no email associated", username);
            }
        });
    }

    public AuthUser loginWithToken(String token) {
        log.info("Verifying magic link token: {}", token);
        AuthUser user = authUserRepository.findByRecoveryToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid recovery link"));

        if (user.getRecoveryTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Recovery link has expired");
        }

        // Clear token after use for security
        user.setRecoveryToken(null);
        user.setRecoveryTokenExpiry(null);
        authUserRepository.save(user);

        log.info("User {} logged in successfully via magic link", user.getName());
        return user;
    }

    public AuthUser resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password for token: {}", request.getToken());
        AuthUser user = authUserRepository.findByRecoveryToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset link"));

        if (user.getRecoveryTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset link has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRecoveryToken(null);
        user.setRecoveryTokenExpiry(null);

        authUserRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getName());
        return user;
    }
}
