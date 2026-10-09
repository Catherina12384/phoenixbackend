package com.phoenix.dto;

import com.phoenix.validation.StrongPassword;
import com.phoenix.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin creates a STAFF account with a temporary password; the staff member must change it at first login. */
public record CreateStaffRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @ValidPhone String phone,
        @NotBlank @StrongPassword String temporaryPassword) {}
