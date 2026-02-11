package com.company.mts.dto;

public class LoginResponse {
    private Long userId;
    private String email;
    private String name;
    private String token;
    private String tokenType = "Bearer";

    public LoginResponse(Long userId, String email, String name, String token) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
