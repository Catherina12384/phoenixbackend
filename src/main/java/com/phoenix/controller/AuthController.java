package com.phoenix.controller;

import com.phoenix.dto.*;
import com.phoenix.security.AuthCookieService;
import com.phoenix.security.AuthenticatedUser;
import com.phoenix.service.AuthService;
import com.phoenix.service.PasswordResetService;
import com.phoenix.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RegistrationService registration;
    private final PasswordResetService passwordReset;
    private final AuthCookieService cookies;

    public AuthController(AuthService authService, RegistrationService registration,
                          PasswordResetService passwordReset, AuthCookieService cookies) {
        this.authService = authService;
        this.registration = registration;
        this.passwordReset = passwordReset;
        this.cookies = cookies;
    }

    /** Signup step 1: validates, sends the OTP, returns a registrationId. */
    @PostMapping("/register/request-otp")
    public RegistrationOtpResponse requestRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        return registration.requestOtp(request);
    }

    /** Signup step 2: verifies the OTP, creates the account and starts the session (cookie). */
    @PostMapping("/register/verify")
    public ResponseEntity<SessionResponse> verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request) {
        return startSession(HttpStatus.CREATED, registration.verify(request));
    }

    /** Email or phone + password. The JWT goes into an httpOnly cookie, not the body. */
    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        return startSession(HttpStatus.OK, authService.login(request));
    }

    /** Public on purpose: it must work even when the session is already invalid. Revokes all devices if logged in. */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal AuthenticatedUser actor) {
        authService.logout(actor);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new MessageResponse("Signed out."));
    }

    /** Always answers the same message, whether or not the phone is registered. */
    @PostMapping("/forgot-password/request-otp")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordReset.requestOtp(request.phone());
        return new MessageResponse("If an account exists for this number, an OTP has been sent.");
    }

    @PostMapping("/forgot-password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.reset(request);
        return new MessageResponse("Password updated. Please log in with your new password.");
    }

    /** Open to any logged-in user, including staff who still have a temporary password. Replaces the cookie. */
    @PostMapping("/change-password")
    public ResponseEntity<SessionResponse> changePassword(Authentication authentication,
                                                          @Valid @RequestBody ChangePasswordRequest request) {
        return startSession(HttpStatus.OK, authService.changePassword(authentication.getName(), request));
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return authService.me(authentication.getName());
    }

    private ResponseEntity<SessionResponse> startSession(HttpStatus status, AuthResponse auth) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookies.issue(auth.token()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new SessionResponse(auth.user()));
    }
}