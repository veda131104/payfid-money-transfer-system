package com.company.mts.controller;

import com.company.mts.dto.AuthResponse;
import com.company.mts.dto.*;
import com.company.mts.dto.CredentialsResponse;
import com.company.mts.entity.AuthUser;
import com.company.mts.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthUser user = authService.signup(request);
        AuthResponse response = new AuthResponse(user.getName(), null, true);
        System.out.println("DEBUG: Signup response created with firstLogin=" + response.isFirstLogin());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthUser user = authService.login(request);
        AuthResponse response = new AuthResponse(user.getName(), user.getRememberToken(), user.isFirstLogin(),
                user.getRole());
        System.out.println("DEBUG: Login response created with firstLogin=" + response.isFirstLogin());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/remember-me")
    public ResponseEntity<CredentialsResponse> getCredentials(@RequestParam String token) {
        return ResponseEntity.ok(authService.getCredentialsByToken(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-token")
    public ResponseEntity<AuthResponse> verifyToken(@RequestParam String token) {
        AuthUser user = authService.loginWithToken(token);
        return ResponseEntity.ok(new AuthResponse(user.getName()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthUser user = authService.resetPassword(request);
        return ResponseEntity.ok(new AuthResponse(user.getName()));
    }
}
