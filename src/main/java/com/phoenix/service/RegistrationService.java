package com.phoenix.service;

import com.phoenix.dto.AuthResponse;
import com.phoenix.dto.RegisterRequest;
import com.phoenix.dto.RegistrationOtpResponse;
import com.phoenix.dto.VerifyRegistrationRequest;

public interface RegistrationService {
    /** Step 1: validate, send OTP, remember the signup (password already hashed). */
    RegistrationOtpResponse requestOtp(RegisterRequest request);

    /** Step 2: check the OTP, create the account, return a token. */
    AuthResponse verify(VerifyRegistrationRequest request);
}
