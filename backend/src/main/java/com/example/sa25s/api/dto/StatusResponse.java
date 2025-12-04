package com.example.sa25s.api.dto;

public class StatusResponse {
    public String status;
    public String detail;

    public static StatusResponse of(String status, String detail) {
        var r = new StatusResponse();
        r.status = status;
        r.detail = detail;
        return r;
    }

    public LoginResponse toLoginResponse() {
        var r = new LoginResponse();
        r.message = detail;
        r.twoFactorRequired = false;
        return r;
    }
}
