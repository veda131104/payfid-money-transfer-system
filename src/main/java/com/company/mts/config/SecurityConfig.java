package com.company.mts.config;

import com.company.mts.security.JwtAuthenticationFilter;
import com.company.mts.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        log.info("[SecurityConfig] Initialized with JwtTokenProvider and UserDetailsService");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("[SecurityConfig] Creating BCryptPasswordEncoder bean");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        log.info("[SecurityConfig] Creating AuthenticationManager bean");
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        log.info("[SecurityConfig] Creating JwtAuthenticationFilter bean");
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[SecurityConfig] Configuring SecurityFilterChain...");
        log.info("[SecurityConfig] Public endpoints: /api/v1/auth/signup, /api/v1/auth/login, /api/v1/auth/login-with-token");
        log.info("[SecurityConfig] Public endpoints: /api/v1/auth/forgot-password, /api/v1/auth/reset-password/**");
        log.info("[SecurityConfig] Public endpoints: /api/v1/account-setup/send-otp, /api/v1/account-setup/verify-otp");
        log.info("[SecurityConfig] Public endpoints: POST /api/v1/account-setup, /api/v1/rewards/**");

        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints for authentication
                        .requestMatchers("/api/v1/auth/signup").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/login-with-token").permitAll()
                        .requestMatchers("/api/v1/auth/forgot-password").permitAll()
                        .requestMatchers("/api/v1/auth/reset-password/**").permitAll()
                        .requestMatchers("/api/v1/auth/verify-token").permitAll()
                        .requestMatchers("/api/v1/auth/credentials/**").permitAll()
                        // Public endpoints for account setup (OTP and account creation)
                        .requestMatchers("/api/v1/account-setup/send-otp").permitAll()
                        .requestMatchers("/api/v1/account-setup/verify-otp").permitAll()
                        .requestMatchers("POST", "/api/v1/account-setup").permitAll()
                        // H2 console for dev
                        .requestMatchers("/h2-console/**").permitAll()
                        // Reward module endpoints (accessible with or without JWT)
                        .requestMatchers("/api/v1/rewards/**").permitAll()
                        // Require authentication for all other endpoints
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {}) // Enable BasicAuth
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                )
                // Allow H2 console frames
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        // Add JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        log.info("[SecurityConfig] SecurityFilterChain configured successfully");
        return http.build();
    }
}
