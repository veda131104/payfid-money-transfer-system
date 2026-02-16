package com.company.mts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponse {
    private String name;
    private String rememberToken;
    @JsonProperty("firstLogin")
    private boolean firstLogin;
    private String role; // Added the missing 'role' field

    public AuthResponse(String name) {
        this.name = name;
        this.firstLogin = false;
        this.role = "USER";
    }

    public AuthResponse(String name, String rememberToken) {
        this.name = name;
        this.rememberToken = rememberToken;
        this.firstLogin = false;
        this.role = "USER";
    }

    public AuthResponse(String name, String rememberToken, boolean firstLogin) {
        this.name = name;
        this.rememberToken = rememberToken;
        this.firstLogin = firstLogin;
        this.role = "USER";
    }

    public AuthResponse(String name, String rememberToken, boolean firstLogin, String role) {
        this.name = name;
        this.rememberToken = rememberToken;
        this.firstLogin = firstLogin;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRememberToken() {
        return rememberToken;
    }

    public void setRememberToken(String rememberToken) {
        this.rememberToken = rememberToken;
    }

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }
}
