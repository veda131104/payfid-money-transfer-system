package com.company.mts.controller;

import com.company.mts.dto.*;
import com.company.mts.dto.CredentialsResponse;
import com.company.mts.dto.AuthResponse;
import com.company.mts.dto.LoginRequest;
import com.company.mts.dto.SignupRequest;
import com.company.mts.dto.LoginResponse;
import com.company.mts.entity.AuthUser;
import com.company.mts.service.AuthService;
import com.company.mts.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        log.info("[AuthController] Initialized with AuthService and JwtTokenProvider");
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("[AuthController] POST /signup - Received signup request for name='{}', email='{}'",
                request.getName(), request.getEmail());
        try {
            AuthUser user = authService.signup(request);
            AuthResponse resp = new AuthResponse(user.getName());
            resp.setEmail(user.getEmail());
            log.info("[AuthController] POST /signup - Signup SUCCESS for name='{}', email='{}'",
                    user.getName(), user.getEmail());
            return new ResponseEntity<>(resp, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("[AuthController] POST /signup - Signup FAILED for name='{}': {} - {}",
                    request.getName(), e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("[AuthController] POST /login - Received login request for name='{}'", request.getName());
        try {
            AuthUser user = authService.login(request);
            String token = jwtTokenProvider.generateToken(user);
            LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, user.getRememberToken());
            resp.setFirstLogin(user.isFirstLogin());
            log.info("[AuthController] POST /login - Login SUCCESS for name='{}', userId={}, firstLogin={}",
                    user.getName(), user.getId(), user.isFirstLogin());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("[AuthController] POST /login - Login FAILED for name='{}': {} - {}",
                    request.getName(), e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login-with-token")
    public ResponseEntity<LoginResponse> loginWithToken(@Valid @RequestBody TokenRequest request) {
        log.info("[AuthController] POST /login-with-token - Received token login request");
        try {
            AuthUser user = authService.loginWithToken(request.getToken());
            String token = jwtTokenProvider.generateToken(user);
            LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, null);
            log.info("[AuthController] POST /login-with-token - Token login SUCCESS for name='{}'", user.getName());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("[AuthController] POST /login-with-token - Token login FAILED: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/credentials/{token}")
    public ResponseEntity<CredentialsResponse> getCredentials(@PathVariable String token) {
        log.info("[AuthController] GET /credentials/{} - Fetching credentials by remember token", token);
        try {
            CredentialsResponse creds = authService.getCredentialsByToken(token);
            log.info("[AuthController] GET /credentials - Credentials retrieved successfully");
            return ResponseEntity.ok(creds);
        } catch (Exception e) {
            log.error("[AuthController] GET /credentials - FAILED: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("[AuthController] POST /forgot-password - Processing forgot password for name='{}'", request.getName());
        try {
            authService.processForgotPassword(request.getName());
            log.info("[AuthController] POST /forgot-password - Forgot password processed for name='{}'", request.getName());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[AuthController] POST /forgot-password - FAILED for name='{}': {} - {}",
                    request.getName(), e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/verify-token")
    public ResponseEntity<LoginResponse> verifyToken(@RequestParam String token) {
        log.info("[AuthController] GET /verify-token - Verifying token");
        try {
            AuthUser user = authService.loginWithToken(token);
            String jwtToken = jwtTokenProvider.generateToken(user);
            LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), jwtToken, null);
            log.info("[AuthController] GET /verify-token - Token verified for name='{}'", user.getName());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("[AuthController] GET /verify-token - FAILED: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<LoginResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("[AuthController] POST /reset-password - Processing password reset");
        try {
            AuthUser user = authService.resetPassword(request);
            String token = jwtTokenProvider.generateToken(user);
            LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, null);
            log.info("[AuthController] POST /reset-password - Password reset SUCCESS for name='{}'", user.getName());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("[AuthController] POST /reset-password - FAILED: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
