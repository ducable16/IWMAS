package com.roamtrip.controller;

import com.roamtrip.auth.UserService;
import com.roamtrip.auth.dto.ChangePasswordRequest;
import com.roamtrip.auth.dto.UpdateProfileRequest;
import com.roamtrip.auth.dto.UserMeResponse;
import com.roamtrip.utils.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserMeController {

    private final UserService userService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping
    public ResponseEntity<UserMeResponse> getMe() {
        return ResponseEntity.ok(userService.getMe(authenticatedUserResolver.currentUserId()));
    }

    @PatchMapping
    public ResponseEntity<UserMeResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(authenticatedUserResolver.currentUserId(), request));
    }

    @PatchMapping("/password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(authenticatedUserResolver.currentUserId(), request));
    }
}
