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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthUser user = authService.signup(request);
        AuthResponse resp = new AuthResponse(user.getName());
        resp.setEmail(user.getEmail());
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthUser user = authService.login(request);
        String token = jwtTokenProvider.generateToken(user);
        LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, user.getRememberToken());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login-with-token")
    public ResponseEntity<LoginResponse> loginWithToken(@Valid @RequestBody TokenRequest request) {
        AuthUser user = authService.loginWithToken(request.getToken());
        String token = jwtTokenProvider.generateToken(user);
        LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, null);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/credentials/{token}")
    public ResponseEntity<CredentialsResponse> getCredentials(@PathVariable String token) {
        return ResponseEntity.ok(authService.getCredentialsByToken(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-token")
    public ResponseEntity<LoginResponse> verifyToken(@RequestParam String token) {
        AuthUser user = authService.loginWithToken(token);
        String jwtToken = jwtTokenProvider.generateToken(user);
        LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), jwtToken, null);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<LoginResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthUser user = authService.resetPassword(request);
        String token = jwtTokenProvider.generateToken(user);
        LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, null);
        return ResponseEntity.ok(resp);
    }
}
