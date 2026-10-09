package com.phoenix.otp;

import com.phoenix.common.Masks;
import com.phoenix.exception.OtpCooldownException;
import com.phoenix.exception.OtpDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class DefaultOtpService implements OtpService {
    private static final Logger log = LoggerFactory.getLogger(DefaultOtpService.class);

    private final OtpGenerator generator;
    private final OtpHasher hasher;
    private final OtpStore store;
    private final OtpSender sender;
    private final OtpAbuseGuard guard;
    private final OtpProperties props;
    private final Clock clock;
    private final TransactionTemplate verifyTx;

    public DefaultOtpService(OtpGenerator generator, OtpHasher hasher, OtpStore store, OtpSender sender,
                             OtpAbuseGuard guard, OtpProperties props, Clock clock, PlatformTransactionManager tm) {
        this.generator = generator;
        this.hasher = hasher;
        this.store = store;
        this.sender = sender;
        this.guard = guard;
        this.props = props;
        this.clock = clock;
        this.verifyTx = new TransactionTemplate(tm);
        this.verifyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void issue(OtpPurpose purpose, String phone) {
        guard.beforeIssue(purpose, phone);

        String otp = generator.generate();
        Instant now = clock.instant();
        boolean stored = store.saveUnlessCoolingDown(purpose, phone, hasher.hash(purpose, phone, otp),
                now, now.plus(props.ttl()), now.minus(props.resendCooldown()));
        if (!stored) {
            long wait = store.find(purpose, phone)
                    .map(s -> (Duration.between(now, s.createdAt().plus(props.resendCooldown())).toMillis() + 999) / 1000)
                    .orElse(props.resendCooldown().toSeconds());
            log.info("OTP resend blocked by cooldown for {} ({})", Masks.phone(phone), purpose);
            throw new OtpCooldownException(wait);
        }

        try {
            sender.send(phone, otp, purpose);
        } catch (OtpDeliveryException e) {
            store.delete(purpose, phone);      // also clears the cooldown so the user can retry straight away
            throw e;
        }
        log.info("OTP issued for {} ({})", Masks.phone(phone), purpose);
    }

    @Override
    public VerificationResult verify(OtpPurpose purpose, String phone, String otp) {
        guard.beforeVerify();      // before the transaction opens: no nested connections

        VerificationResult result = verifyTx.execute(status -> {
            var found = store.findForUpdate(purpose, phone);
            if (found.isEmpty()) return VerificationResult.EXPIRED;
            StoredOtp stored = found.get();

            if (!stored.expiresAt().isAfter(clock.instant())) {
                store.delete(purpose, phone);
                return VerificationResult.EXPIRED;
            }
            if (hasher.matches(purpose, phone, otp, stored.codeHash())) {
                store.delete(purpose, phone);              // single use
                return VerificationResult.VERIFIED;
            }
            int attempts = stored.attempts() + 1;
            if (attempts >= props.maxAttempts()) {
                store.delete(purpose, phone);              // destroyed: a new OTP is required
                return VerificationResult.LOCKED_OUT;
            }
            store.updateAttempts(stored.id(), attempts);
            return VerificationResult.INVALID;
        });

        if (result == VerificationResult.LOCKED_OUT) guard.onLockout(purpose, phone);
        log.info("OTP verification for {} ({}): {}", Masks.phone(phone), purpose, result);
        return result;
    }
}
