package com.company.mts.service;

import com.company.mts.dto.LoginRequest;
import com.company.mts.dto.LoginResponse;
import com.company.mts.dto.SignupRequest;
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

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public AuthUser signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (authUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException("Email already in use");
        }

        AuthUser user = new AuthUser();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        log.info("Saving new user to database: email={}", email);
        return authUserRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        AuthUser user = authUserRepository
                .findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // Verify password with BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Generate JWT token
        String token = tokenProvider.generateToken(user);

        return new LoginResponse(user.getId(), user.getEmail(), user.getName(), token);
    }
}
