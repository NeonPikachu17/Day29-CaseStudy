package com.ewb.common.dto;

public class AuthRequest {

    private String username;

    public AuthRequest() {
    }

    public AuthRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
