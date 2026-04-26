package com.iwas.auth;

import com.iwas.auth.dto.AuthResponse;
import com.iwas.auth.dto.ForgotPasswordRequest;
import com.iwas.auth.dto.LoginRequest;
import com.iwas.auth.dto.RegisterRequest;
import com.iwas.auth.dto.ResetPasswordRequest;
import com.iwas.auth.dto.SendOtpRequest;
import com.iwas.auth.dto.VerifyOtpRequest;
import com.iwas.security.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // Giữ ResponseEntity vì cần set Location header cho redirect
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendBaseUrl + "/login?verified=true")
                .build();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public String logout() {
        authService.logout(authenticatedUserResolver.currentSessionId());
        return "Logged out";
    }

    @PostMapping("/send-otp")
    public String sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return authService.sendOtp(request);
    }

    @PostMapping("/verify-email-otp")
    public String verifyEmailOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyEmailOtp(request);
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
