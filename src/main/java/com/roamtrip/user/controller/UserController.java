package com.roamtrip.user.controller;

import com.roamtrip.auth.dto.ChangePasswordRequest;
import com.roamtrip.auth.dto.UpdateProfileRequest;
import com.roamtrip.security.AuthenticatedUserResolver;
import com.roamtrip.user.dto.UserMeResponse;
import com.roamtrip.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe() {
        return ResponseEntity.ok(userService.getMe(authenticatedUserResolver.currentUserId()));
    }

    @PostMapping("/me/update")
    public ResponseEntity<UserMeResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(authenticatedUserResolver.currentUserId(), request));
    }

    @PostMapping("/me/password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(authenticatedUserResolver.currentUserId(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<UserMeResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public ResponseEntity<UserMeResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserMeResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserActive(id, true));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserMeResponse> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserActive(id, false));
    }
}
