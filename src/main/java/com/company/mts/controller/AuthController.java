package com.company.mts.controller;

import com.company.mts.dto.*;
import com.company.mts.entity.AuthUser;
import com.company.mts.service.AuthService;
import com.company.mts.security.CryptoService;
import com.company.mts.security.JwtTokenProvider;
import com.company.mts.repository.AuthUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CryptoService cryptoService;
    private final AuthUserRepository authUserRepository;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider,
                          CryptoService cryptoService, AuthUserRepository authUserRepository) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cryptoService = cryptoService;
        this.authUserRepository = authUserRepository;
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
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        authUserRepository.save(user);
        LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, user.getRememberToken());
        resp.setRefreshToken(refreshToken);
        resp.setFirstLogin(user.isFirstLogin());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login-with-token")
    public ResponseEntity<LoginResponse> loginWithToken(@Valid @RequestBody TokenRequest request) {
        AuthUser user = authService.loginWithToken(request.getToken());
        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        authUserRepository.save(user);
        LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, null);
        resp.setRefreshToken(refreshToken);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        AuthUser user = authUserRepository.findByNameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRefreshTokenExpiry() == null || user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Refresh token expired"));
        }
        String newToken = jwtTokenProvider.generateToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        authUserRepository.save(user);
        return ResponseEntity.ok(Map.of(
            "token", newToken,
            "refreshToken", newRefreshToken,
            "tokenType", "Bearer"
        ));
    }

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", cryptoService.getPublicKeyBase64()));
    }

    @GetMapping("/credentials/{token}")
    public ResponseEntity<CredentialsResponse> getCredentials(@PathVariable String token) {
        CredentialsResponse creds = authService.getCredentialsByToken(token);
        return ResponseEntity.ok(creds);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPassword(request.getName());
        String msg = "If an account exists for the provided username, a password reset link has been sent to the registered email address.";
        return ResponseEntity.ok(new ForgotPasswordResponse(msg));
    }

    @GetMapping("/verify-token")
    public ResponseEntity<LoginResponse> verifyToken(@RequestParam String token) {
        AuthUser user = authService.loginWithToken(token);
        String jwtToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        authUserRepository.save(user);
        LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), jwtToken, null);
        resp.setRefreshToken(refreshToken);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<LoginResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthUser user = authService.resetPassword(request);
        String token = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        authUserRepository.save(user);
        LoginResponse resp = new LoginResponse(user.getId(), user.getEmail(), user.getName(), token, null);
        resp.setRefreshToken(refreshToken);
        return ResponseEntity.ok(resp);
    }
}
