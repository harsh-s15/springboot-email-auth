package com.example.auth.bean;

public class PasswordResetToken {

    private String token;
    private String username;
    private long expiryEpoch;

    public PasswordResetToken() {}

    public PasswordResetToken(String token, String username, long expiryEpoch) {
        this.token = token;
        this.username = username;
        this.expiryEpoch = expiryEpoch;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public long getExpiryEpoch() { return expiryEpoch; }

    public void setToken(String token) { this.token = token; }
    public void setUsername(String username) { this.username = username; }
    public void setExpiryEpoch(long expiryEpoch) { this.expiryEpoch = expiryEpoch; }
}