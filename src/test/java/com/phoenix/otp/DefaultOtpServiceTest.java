package com.phoenix.otp;

import com.phoenix.exception.OtpCooldownException;
import com.phoenix.exception.OtpDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultOtpServiceTest {
    static final String PHONE = "+919500288164";
    static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

    OtpGenerator generator = mock(OtpGenerator.class);
    OtpStore store = mock(OtpStore.class);
    OtpSender sender = mock(OtpSender.class);
    OtpAbuseGuard guard = mock(OtpAbuseGuard.class);
    OtpProperties props = HmacSha256OtpHasherTest.props("s".repeat(32));
    HmacSha256OtpHasher hasher = new HmacSha256OtpHasher(props);
    DefaultOtpService service;

    @BeforeEach
    void setUp() {
        when(generator.generate()).thenReturn("123456");
        service = new DefaultOtpService(generator, hasher, store, sender, guard, props,
                Clock.fixed(NOW, ZoneOffset.UTC), mock(PlatformTransactionManager.class));
    }

    // ---------- issue ----------
    @Test void issueStoresHashedCodeAndSends() {
        when(store.saveUnlessCoolingDown(any(), any(), any(), any(), any(), any())).thenReturn(true);

        service.issue(OtpPurpose.REGISTER, PHONE);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Instant> expires = ArgumentCaptor.forClass(Instant.class);
        verify(store).saveUnlessCoolingDown(eq(OtpPurpose.REGISTER), eq(PHONE), hash.capture(), eq(NOW),
                expires.capture(), eq(NOW.minus(Duration.ofSeconds(60))));
        assertNotEquals("123456", hash.getValue());                      // never stored in plain text
        assertEquals(NOW.plus(Duration.ofMinutes(5)), expires.getValue()); // 5-minute expiry
        verify(sender).send(PHONE, "123456", OtpPurpose.REGISTER);
    }

    @Test void issueAppliesAbuseGuardFirst() {
        doThrow(new RuntimeException("blocked")).when(guard).beforeIssue(any(), any());
        assertThrows(RuntimeException.class, () -> service.issue(OtpPurpose.REGISTER, PHONE));
        verifyNoInteractions(sender, store);
    }

    @Test void issueRejectedDuringCooldownAndSendsNothing() {
        when(store.saveUnlessCoolingDown(any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(store.find(OtpPurpose.REGISTER, PHONE))
                .thenReturn(Optional.of(new StoredOtp(1, "h", 0, NOW.minusSeconds(20), NOW.plusSeconds(280))));

        OtpCooldownException e = assertThrows(OtpCooldownException.class, () -> service.issue(OtpPurpose.REGISTER, PHONE));
        assertEquals(40, e.getRetryAfterSeconds());
        verifyNoInteractions(sender);
    }

    @Test void deliveryFailureDeletesOtpSoUserCanRetry() {
        when(store.saveUnlessCoolingDown(any(), any(), any(), any(), any(), any())).thenReturn(true);
        doThrow(new OtpDeliveryException("down")).when(sender).send(any(), any(), any());

        assertThrows(OtpDeliveryException.class, () -> service.issue(OtpPurpose.REGISTER, PHONE));
        verify(store).delete(OtpPurpose.REGISTER, PHONE);
    }

    // ---------- verify ----------
    private StoredOtp stored(int attempts, Instant expiresAt) {
        return new StoredOtp(7, hasher.hash(OtpPurpose.REGISTER, PHONE, "123456"), attempts, NOW.minusSeconds(90), expiresAt);
    }

    @Test void correctCodeVerifiesAndIsSingleUse() {
        when(store.findForUpdate(OtpPurpose.REGISTER, PHONE)).thenReturn(Optional.of(stored(0, NOW.plusSeconds(200))));
        assertEquals(VerificationResult.VERIFIED, service.verify(OtpPurpose.REGISTER, PHONE, "123456"));
        verify(store).delete(OtpPurpose.REGISTER, PHONE);
    }

    @Test void wrongCodeCountsAnAttempt() {
        when(store.findForUpdate(OtpPurpose.REGISTER, PHONE)).thenReturn(Optional.of(stored(0, NOW.plusSeconds(200))));
        assertEquals(VerificationResult.INVALID, service.verify(OtpPurpose.REGISTER, PHONE, "000000"));
        verify(store).updateAttempts(7, 1);
        verify(store, never()).delete(any(), any());
    }

    @Test void thirdWrongCodeDestroysOtpAndLocksOut() {
        when(store.findForUpdate(OtpPurpose.REGISTER, PHONE)).thenReturn(Optional.of(stored(2, NOW.plusSeconds(200))));
        assertEquals(VerificationResult.LOCKED_OUT, service.verify(OtpPurpose.REGISTER, PHONE, "000000"));
        verify(store).delete(OtpPurpose.REGISTER, PHONE);
        verify(guard).onLockout(OtpPurpose.REGISTER, PHONE);
    }

    @Test void expiredOtpIsRejectedEvenIfCorrect() {
        when(store.findForUpdate(OtpPurpose.REGISTER, PHONE)).thenReturn(Optional.of(stored(0, NOW.minusSeconds(1))));
        assertEquals(VerificationResult.EXPIRED, service.verify(OtpPurpose.REGISTER, PHONE, "123456"));
        verify(store).delete(OtpPurpose.REGISTER, PHONE);
    }

    @Test void missingOtpIsExpired() {
        when(store.findForUpdate(any(), any())).thenReturn(Optional.empty());
        assertEquals(VerificationResult.EXPIRED, service.verify(OtpPurpose.REGISTER, PHONE, "123456"));
    }

    @Test void verifyAppliesIpRateLimitFirst() {
        doThrow(new RuntimeException("limited")).when(guard).beforeVerify();
        assertThrows(RuntimeException.class, () -> service.verify(OtpPurpose.REGISTER, PHONE, "123456"));
        verifyNoInteractions(store);
    }
}
