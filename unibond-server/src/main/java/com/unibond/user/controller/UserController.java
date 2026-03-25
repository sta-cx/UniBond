package com.unibond.user.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.security.UserPrincipal;
import com.unibond.user.dto.ProfileUpdateRequest;
import com.unibond.user.dto.UserResponse;
import com.unibond.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getMe(principal.userId()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest req) {
        return ApiResponse.ok(userService.updateProfile(principal.userId(), req.nickname(), req.avatarUrl()));
    }

    @PostMapping("/device-token")
    public ApiResponse<Void> updateDeviceToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody java.util.Map<String, String> body) {
        userService.updateDeviceToken(principal.userId(), body.get("deviceToken"));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteAccount(principal.userId());
        return ApiResponse.ok(null);
    }
}
