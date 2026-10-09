package com.phoenix.otp;

import java.time.Instant;

public record StoredOtp(long id, String codeHash, int attempts, Instant createdAt, Instant expiresAt) {}
