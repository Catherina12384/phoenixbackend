package com.phoenix.dto;

import com.phoenix.validation.StrongPassword;
import com.phoenix.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Every field is optional; null means "leave unchanged". Roles cannot be changed. */
public record UserUpdateRequest(
        @Size(max = 120) String name,
        @Email @Size(max = 255) String email,
        @ValidPhone String phone,
        Boolean active,
        @StrongPassword String temporaryPassword) {}
