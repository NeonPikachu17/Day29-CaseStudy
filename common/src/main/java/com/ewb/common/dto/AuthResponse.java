package com.ewb.common.dto;

import java.util.List;

public class AuthResponse {

    private String token;
    private String username;
    private String fullName;
    private String role;
    private List<String> accountIds;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, String fullName, String role, List<String> accountIds) {
        this.token = token;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.accountIds = accountIds;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<String> accountIds) {
        this.accountIds = accountIds;
    }
}
