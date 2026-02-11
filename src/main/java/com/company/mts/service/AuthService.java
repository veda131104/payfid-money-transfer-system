package com.company.mts.service;

import com.company.mts.dto.LoginRequest;
import com.company.mts.dto.SignupRequest;
import com.company.mts.entity.AuthUser;
import com.company.mts.exception.DuplicateUserException;
import com.company.mts.exception.InvalidCredentialsException;
import com.company.mts.repository.AuthUserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;

    public AuthService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    public AuthUser signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (authUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException("Email already in use");
        }

        AuthUser user = new AuthUser();
        user.setName(request.getName());
        user.setEmail(email);
        user.setPassword(request.getPassword());
        return authUserRepository.save(user);
    }

    public AuthUser login(LoginRequest request) {
        AuthUser user = authUserRepository
                .findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return user;
    }
}
