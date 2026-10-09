package com.phoenix.service;

import com.phoenix.dto.ResetPasswordRequest;

public interface PasswordResetService {
    /** Always completes silently for unknown phones, so callers cannot discover which numbers are registered. */
    void requestOtp(String phone);

    void reset(ResetPasswordRequest request);
}
