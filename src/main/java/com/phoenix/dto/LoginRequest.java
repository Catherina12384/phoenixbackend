package com.phoenix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code identifier} is an email address or an Indian mobile number; the password is checked either way. */
public record LoginRequest(
        @NotBlank @Size(max = 255) String identifier,
        @NotBlank @Size(max = 128) String password) {}