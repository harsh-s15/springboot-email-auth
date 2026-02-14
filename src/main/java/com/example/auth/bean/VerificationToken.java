package com.example.auth.bean;

public class VerificationToken {

    private String token;
    private String username;
    private long expiryEpoch;

    public VerificationToken() {}

    public VerificationToken(String token, String username, long expiryEpoch) {
        this.token = token;
        this.username = username;
        this.expiryEpoch = expiryEpoch;
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

    public long getExpiryEpoch() {
        return expiryEpoch;
    }

    public void setExpiryEpoch(long expiryEpoch) {
        this.expiryEpoch = expiryEpoch;
    }
}
