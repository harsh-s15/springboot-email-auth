package com.example.auth.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
