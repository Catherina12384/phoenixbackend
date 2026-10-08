package com.phoenix.service;

import com.phoenix.dto.AuthResponse;
import com.phoenix.dto.LoginRequest;
import com.phoenix.dto.RegisterRequest;
import com.phoenix.dto.UserDto;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserDto me(String email);
}
