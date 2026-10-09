package com.phoenix.controller;

import com.phoenix.dto.*;
import com.phoenix.service.AuthService;
import com.phoenix.service.PasswordResetService;
import com.phoenix.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RegistrationService registration;
    private final PasswordResetService passwordReset;

    public AuthController(AuthService authService, RegistrationService registration, PasswordResetService passwordReset) {
        this.authService = authService;
        this.registration = registration;
        this.passwordReset = passwordReset;
    }

    /** Signup step 1: validates, sends the OTP, returns a registrationId. */
    @PostMapping("/register/request-otp")
    public RegistrationOtpResponse requestRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        return registration.requestOtp(request);
    }

    /** Signup step 2: verifies the OTP, creates the account, returns a token. */
    @PostMapping("/register/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request) {
        return registration.verify(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
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

    /** Open to any logged-in user, including staff who still have a temporary password. */
    @PostMapping("/change-password")
    public AuthResponse changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(authentication.getName(), request);
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
