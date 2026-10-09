package com.phoenix.otp;

import java.time.Instant;
import java.util.Optional;

public interface OtpStore {
    /**
     * Atomically stores a new challenge (replacing any older one) unless the existing one was created
     * after {@code cooldownThreshold}. Returns false when the resend cooldown is still running.
     */
    boolean saveUnlessCoolingDown(OtpPurpose purpose, String phone, String codeHash,
                                  Instant createdAt, Instant expiresAt, Instant cooldownThreshold);

    Optional<StoredOtp> find(OtpPurpose purpose, String phone);

    /** Row lock; must run inside a transaction. Serialises concurrent verification attempts. */
    Optional<StoredOtp> findForUpdate(OtpPurpose purpose, String phone);

    void updateAttempts(long id, int attempts);

    void delete(OtpPurpose purpose, String phone);
}
