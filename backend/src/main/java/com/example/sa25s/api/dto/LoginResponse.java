package com.example.sa25s.api.dto;

public class LoginResponse {
    public boolean twoFactorRequired;
    public String token;
    public String temporaryToken;
    public String message;

    public static LoginResponse immediateToken(String token) {
        var response = new LoginResponse();
        response.twoFactorRequired = false;
        response.token = token;
        response.message = "Login successful without 2FA";
        return response;
    }

    public static LoginResponse pending2fa(String tempToken) {
        var response = new LoginResponse();
        response.twoFactorRequired = true;
        response.temporaryToken = tempToken;
        response.message = "2FA required. Confirm the OTP.";
        return response;
    }
}
