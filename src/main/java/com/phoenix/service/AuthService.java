package com.phoenix.service;

import com.phoenix.dto.AuthResponse;
import com.phoenix.dto.ChangePasswordRequest;
import com.phoenix.dto.LoginRequest;
import com.phoenix.dto.UserDto;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    UserDto me(String email);

    /** Returns a fresh token; the old one stops working. */
    AuthResponse changePassword(String email, ChangePasswordRequest request);
}
