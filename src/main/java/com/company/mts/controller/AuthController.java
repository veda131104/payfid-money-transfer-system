package com.company.mts.controller;

import com.company.mts.dto.AuthResponse;
import com.company.mts.dto.LoginRequest;
import com.company.mts.dto.LoginResponse;
import com.company.mts.dto.SignupRequest;
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
        return new ResponseEntity<>(new AuthResponse(user.getName(), user.getEmail()), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        return ResponseEntity.ok(loginResponse);
    }
}
