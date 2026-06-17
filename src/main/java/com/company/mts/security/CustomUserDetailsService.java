package com.company.mts.security;

import com.company.mts.entity.AuthUser;
import com.company.mts.repository.AuthUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    @Autowired
    public CustomUserDetailsService(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        AuthUser user = authUserRepository.findByNameIgnoreCase(username.trim())
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        return User.builder()
                .username(user.getName())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("Loading user details for userId: {}", userId);

        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new UsernameNotFoundException("User not found with id: " + userId);
                });

        return User.builder()
                .username(user.getName())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
