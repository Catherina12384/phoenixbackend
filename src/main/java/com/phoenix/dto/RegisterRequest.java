package com.phoenix.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.phoenix.validation.StrongPassword;
import com.phoenix.validation.ValidPhone;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @ValidPhone String phone,
        @NotBlank @StrongPassword String password,
        @NotBlank String confirmPassword) {

    @JsonIgnore
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return password == null || password.equals(confirmPassword);
    }
}
