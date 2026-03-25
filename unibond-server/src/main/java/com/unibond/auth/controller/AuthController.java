package com.unibond.auth.controller;

import com.unibond.auth.dto.*;
import com.unibond.auth.service.AppleAuthService;
import com.unibond.auth.service.AuthService;
import com.unibond.auth.service.EmailService;
import com.unibond.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final EmailService emailService;
    private final AppleAuthService appleAuthService;

    public AuthController(AuthService authService, EmailService emailService,
                          AppleAuthService appleAuthService) {
        this.authService = authService;
        this.emailService = emailService;
        this.appleAuthService = appleAuthService;
    }

    @PostMapping("/email/send")
    public ApiResponse<Void> sendCode(@Valid @RequestBody EmailSendRequest req) throws Exception {
        authService.sendEmailCode(req.email());
        String code = authService.getEmailCode(req.email());
        emailService.sendVerificationCode(req.email(), code);
        return ApiResponse.ok(null);
    }

    @PostMapping("/email/login")
    public ApiResponse<AuthResponse> emailLogin(@Valid @RequestBody EmailLoginRequest req) {
        return ApiResponse.ok(authService.emailLogin(req.email(), req.code()));
    }

    @PostMapping("/apple")
    public ApiResponse<AuthResponse> appleLogin(@Valid @RequestBody AppleLoginRequest req) {
        String appleSub = appleAuthService.extractAppleSub(req.identityToken());
        return ApiResponse.ok(authService.appleLogin(appleSub, req.nickname(), req.timezone()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody java.util.Map<String, String> body) {
        return ApiResponse.ok(authService.refresh(body.get("refreshToken")));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody java.util.Map<String, String> body) {
        // Accepts refreshToken in body — access token may be expired
        authService.logoutByRefreshToken(body.get("refreshToken"));
        return ApiResponse.ok(null);
    }
}
