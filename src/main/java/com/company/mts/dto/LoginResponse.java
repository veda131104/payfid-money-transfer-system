package com.company.mts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String email;
    private String name;
    private String token;
    private String tokenType = "Bearer";
    private String rememberToken;
    private boolean firstLogin;
    private String refreshToken;

    public LoginResponse(Long userId, String email, String name, String token, String rememberToken) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.token = token;
        this.tokenType = "Bearer";
        this.rememberToken = rememberToken;
        this.firstLogin = false;
    }
}
