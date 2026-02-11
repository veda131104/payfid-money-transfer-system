package com.company.mts.security;

import com.company.mts.entity.AuthToken;
import com.company.mts.entity.AuthUser;
import com.company.mts.repository.AuthTokenRepository;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationAndValidationInMoneyTransferSystemApplication}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpirationMs;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    /**
     * Generate a JWT token for the given user
     */
    public String generateToken(AuthUser user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String token = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();

        // Save token to database
        AuthToken authToken = new AuthToken();
        authToken.setUser(user);
        authToken.setToken(token);
        authToken.setTokenType("BEARER");
        authToken.setIsValid(true);
        authToken.setExpiresAt(expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        authTokenRepository.save(authToken);

        return token;
    }

    /**
     * Get user ID from the JWT token
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validate the JWT token
     */
    public boolean validateToken(String token) {
        try {
            // Check if token exists in database and is valid
            AuthToken authToken = authTokenRepository.findByToken(token)
                    .orElse(null);

            if (authToken == null || !authToken.isValid()) {
                return false;
            }

            // Validate the JWT signature and expiration
            Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token);

            // Update last used timestamp
            authToken.setLastUsed(LocalDateTime.now());
            authTokenRepository.save(authToken);

            return true;
        } catch (SecurityException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("Expired JWT token: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error validating token: " + e.getMessage());
        }
        return false;
    }

    /**
     * Extract the token from Authorization header
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * Revoke a token
     */
    public void revokeToken(String token) {
        authTokenRepository.findByToken(token).ifPresent(authToken -> {
            authToken.setIsValid(false);
            authToken.setRevokedAt(LocalDateTime.now());
            authTokenRepository.save(authToken);
        });
    }
}

