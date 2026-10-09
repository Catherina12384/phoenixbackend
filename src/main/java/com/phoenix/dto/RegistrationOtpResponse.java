package com.phoenix.dto;

import java.util.UUID;

public record RegistrationOtpResponse(UUID registrationId, long otpValidSeconds, long resendAfterSeconds) {}
