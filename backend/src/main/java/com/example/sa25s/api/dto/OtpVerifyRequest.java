package com.example.sa25s.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpVerifyRequest {

    @NotBlank
    public String temporaryToken;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6 digit code")
    public String otp;
}
