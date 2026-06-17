package com.company.mts.dto;

public class AuthResponse {
    private String name;
    private String email;
    private String rememberToken;

    public AuthResponse(String name) {
        this.name = name;
    }

    public AuthResponse(String name, String rememberToken) {
        this.name = name;
        this.rememberToken = rememberToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRememberToken() {
        return rememberToken;
    }

    public void setRememberToken(String rememberToken) {
        this.rememberToken = rememberToken;
    }
}

