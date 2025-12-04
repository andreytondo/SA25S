package com.example.sa25s.api.dto;

import jakarta.validation.constraints.NotBlank;

public class Disable2faRequest {
    @NotBlank
    public String otp;
}
