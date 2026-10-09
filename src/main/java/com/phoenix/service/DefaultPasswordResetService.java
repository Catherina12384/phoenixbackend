package com.phoenix.service;

import com.phoenix.audit.AuditAction;
import com.phoenix.audit.AuditLogger;
import com.phoenix.common.Masks;
import com.phoenix.common.PhoneNumbers;
import com.phoenix.dto.ResetPasswordRequest;
import com.phoenix.entity.User;
import com.phoenix.exception.OtpCooldownException;
import com.phoenix.exception.OtpDeliveryException;
import com.phoenix.exception.RateLimitExceededException;
import com.phoenix.otp.OtpAbuseGuard;
import com.phoenix.otp.OtpProperties;
import com.phoenix.otp.OtpPurpose;
import com.phoenix.otp.OtpService;
import com.phoenix.otp.VerificationResult;
import com.phoenix.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

/** Not @Transactional for the same reason as registration: OTP counters must always commit. */
@Service
public class DefaultPasswordResetService implements PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(DefaultPasswordResetService.class);
    private static final String GENERIC_FAILURE = "Invalid or expired OTP";

    private final UserRepository users;
    private final OtpService otp;
    private final OtpAbuseGuard guard;
    private final OtpProperties otpProps;
    private final PasswordEncoder encoder;
    private final UserCredentialUpdater credentials;
    private final AuditLogger audit;

    public DefaultPasswordResetService(UserRepository users, OtpService otp, OtpAbuseGuard guard, OtpProperties otpProps,
                                       PasswordEncoder encoder, UserCredentialUpdater credentials, AuditLogger audit) {
        this.users = users;
        this.otp = otp;
        this.guard = guard;
        this.otpProps = otpProps;
        this.encoder = encoder;
        this.credentials = credentials;
        this.audit = audit;
    }

    @Override
    public void requestOtp(String rawPhone) {
        String phone = PhoneNumbers.normalize(rawPhone);
        Optional<User> user = users.findByPhone(phone).filter(User::isActive);

        if (user.isEmpty()) {
            guard.beforeIssue(OtpPurpose.RESET_PASSWORD, phone);   // same rate limits as a real account
            log.info("Password reset requested for unregistered phone {}", Masks.phone(phone));
            return;
        }
        User u = user.get();
        try {
            otp.issue(OtpPurpose.RESET_PASSWORD, phone);
            audit.logAs(u.getEmail(), u.getRole().name(), AuditAction.PASSWORD_RESET_REQUESTED, "user",
                    String.valueOf(u.getId()), true, null);
        } catch (OtpCooldownException e) {
            log.info("Password reset OTP skipped (cooldown) for {}", u.getEmail());   // swallowed: must look identical
        } catch (OtpDeliveryException e) {
            log.error("Password reset OTP could not be delivered to user {}", u.getId());
            audit.logAs(u.getEmail(), u.getRole().name(), AuditAction.PASSWORD_RESET_REQUESTED, "user",
                    String.valueOf(u.getId()), false, Map.of("reason", "delivery_failed"));
        }
    }

    @Override
    public void reset(ResetPasswordRequest req) {
        String phone = PhoneNumbers.normalize(req.phone());
        VerificationResult result = otp.verify(OtpPurpose.RESET_PASSWORD, phone, req.otp());

        if (result == VerificationResult.LOCKED_OUT) {
            audit.logAs(null, "ANONYMOUS", AuditAction.PASSWORD_RESET_FAILED, "phone", Masks.phone(phone), false,
                    Map.of("reason", "locked_out"));
            throw new RateLimitExceededException("Too many incorrect OTP attempts. Try again later.",
                    otpProps.lockout().toSeconds());
        }
        if (result != VerificationResult.VERIFIED) {
            audit.logAs(null, "ANONYMOUS", AuditAction.PASSWORD_RESET_FAILED, "phone", Masks.phone(phone), false,
                    Map.of("reason", result.name().toLowerCase()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, GENERIC_FAILURE);
        }

        User user = users.findByPhone(phone).filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, GENERIC_FAILURE));
        credentials.setPassword(user.getId(), encoder.encode(req.newPassword()), false);
        audit.logAs(user.getEmail(), user.getRole().name(), AuditAction.PASSWORD_RESET_COMPLETED, "user",
                String.valueOf(user.getId()), true, null);
        log.info("Password reset completed for {}", user.getEmail());
    }
}
