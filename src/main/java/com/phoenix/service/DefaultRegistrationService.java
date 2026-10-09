package com.phoenix.service;

import com.phoenix.audit.AuditAction;
import com.phoenix.audit.AuditLogger;
import com.phoenix.common.ClientContext;
import com.phoenix.common.Masks;
import com.phoenix.common.PhoneNumbers;
import com.phoenix.dto.AuthResponse;
import com.phoenix.dto.RegisterRequest;
import com.phoenix.dto.RegistrationOtpResponse;
import com.phoenix.dto.UserDto;
import com.phoenix.dto.VerifyRegistrationRequest;
import com.phoenix.entity.PendingRegistration;
import com.phoenix.entity.User;
import com.phoenix.exception.RateLimitExceededException;
import com.phoenix.otp.OtpProperties;
import com.phoenix.otp.OtpPurpose;
import com.phoenix.otp.OtpService;
import com.phoenix.otp.VerificationResult;
import com.phoenix.ratelimit.RateLimitProperties;
import com.phoenix.ratelimit.RateLimiter;
import com.phoenix.repository.PendingRegistrationRepository;
import com.phoenix.repository.UserRepository;
import com.phoenix.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/** Deliberately NOT @Transactional: OTP attempt counters must commit even when we then reject the request. */
@Service
public class DefaultRegistrationService implements RegistrationService {
    private static final Logger log = LoggerFactory.getLogger(DefaultRegistrationService.class);

    private final UserRepository users;
    private final PendingRegistrationRepository pendingRepo;
    private final RegistrationPersistence persistence;
    private final OtpService otp;
    private final OtpProperties otpProps;
    private final RateLimiter limiter;
    private final RateLimitProperties rates;
    private final ClientContext client;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditLogger audit;
    private final Clock clock;

    public DefaultRegistrationService(UserRepository users, PendingRegistrationRepository pendingRepo,
                                      RegistrationPersistence persistence, OtpService otp, OtpProperties otpProps,
                                      RateLimiter limiter, RateLimitProperties rates, ClientContext client,
                                      PasswordEncoder encoder, JwtService jwt, AuditLogger audit, Clock clock) {
        this.users = users;
        this.pendingRepo = pendingRepo;
        this.persistence = persistence;
        this.otp = otp;
        this.otpProps = otpProps;
        this.limiter = limiter;
        this.rates = rates;
        this.client = client;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public RegistrationOtpResponse requestOtp(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        String phone = PhoneNumbers.normalize(req.phone());

        // The duplicate checks below reveal whether an account exists, so they are rate limited per IP.
        limiter.consume("signup-check:" + client.ip(), rates.otpPerIp());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        if (users.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this phone number already exists");
        }

        otp.issue(OtpPurpose.REGISTER, phone);      // limits, cooldown, SMS. Throws before we touch the pending signup.

        Instant expires = clock.instant().plus(otpProps.ttl());
        PendingRegistration pending = persistence.savePending(req.name().trim(), email, phone,
                encoder.encode(req.password()), expires);

        audit.logAs(email, "ANONYMOUS", AuditAction.REGISTER_OTP_REQUESTED, "registration", pending.getId().toString(),
                true, Map.of("phone", Masks.phone(phone)));
        return new RegistrationOtpResponse(pending.getId(), otpProps.ttl().toSeconds(), otpProps.resendCooldown().toSeconds());
    }

    @Override
    public AuthResponse verify(VerifyRegistrationRequest req) {
        PendingRegistration pending = pendingRepo.findById(req.registrationId())
                .filter(p -> p.getExpiresAt().isAfter(clock.instant()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Registration expired or not found. Please start again."));

        VerificationResult result = otp.verify(OtpPurpose.REGISTER, pending.getPhone(), req.otp());
        switch (result) {
            case VERIFIED -> { }
            case INVALID -> {
                audit.logAs(pending.getEmail(), "ANONYMOUS", AuditAction.REGISTER_OTP_FAILED, "registration",
                        pending.getId().toString(), false, null);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect OTP");
            }
            case EXPIRED -> {
                audit.logAs(pending.getEmail(), "ANONYMOUS", AuditAction.REGISTER_OTP_FAILED, "registration",
                        pending.getId().toString(), false, Map.of("reason", "expired"));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired. Please request a new one.");
            }
            case LOCKED_OUT -> {
                audit.logAs(pending.getEmail(), "ANONYMOUS", AuditAction.REGISTER_OTP_FAILED, "registration",
                        pending.getId().toString(), false, Map.of("reason", "locked_out"));
                throw new RateLimitExceededException("Too many incorrect OTP attempts. Try again later.",
                        otpProps.lockout().toSeconds());
            }
        }

        User user = persistence.complete(pending.getId());
        audit.logAs(user.getEmail(), user.getRole().name(), AuditAction.REGISTER_COMPLETED, "user",
                String.valueOf(user.getId()), true, null);
        log.info("Customer registered: {}", user.getEmail());
        return new AuthResponse(jwt.issue(user), UserDto.from(user));
    }
}
