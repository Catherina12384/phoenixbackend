package com.phoenix.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyRegistrationRequest(
        @NotNull UUID registrationId,
        @NotNull @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otp) {}
