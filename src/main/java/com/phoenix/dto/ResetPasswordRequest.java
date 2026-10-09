package com.phoenix.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.phoenix.validation.StrongPassword;
import com.phoenix.validation.ValidPhone;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank @ValidPhone String phone,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "OTP must be 6 digits") String otp,
        @NotBlank @StrongPassword String newPassword,
        @NotBlank String confirmPassword) {

    @JsonIgnore
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return newPassword == null || newPassword.equals(confirmPassword);
    }
}
