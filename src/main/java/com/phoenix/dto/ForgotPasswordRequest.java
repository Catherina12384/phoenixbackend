package com.phoenix.dto;

import com.phoenix.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank @ValidPhone String phone) {}
