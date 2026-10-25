package com.phoenix.service;

import com.phoenix.dto.AuthResponse;
import com.phoenix.dto.ChangePasswordRequest;
import com.phoenix.dto.LoginRequest;
import com.phoenix.dto.UserDto;
import com.phoenix.security.AuthenticatedUser;

public interface AuthService {
    /** Accepts an email or a phone number. The returned token is for the cookie; it is never put in a response body. */
    AuthResponse login(LoginRequest request);

    UserDto me(String email);

    /** Returns a fresh token; the old one stops working. */
    AuthResponse changePassword(String email, ChangePasswordRequest request);

    /** Revokes every session of the user. A null actor (no valid session) is a no-op. */
    void logout(AuthenticatedUser actor);
}