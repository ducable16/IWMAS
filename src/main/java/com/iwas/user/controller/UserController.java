package com.iwas.user.controller;

import com.iwas.auth.dto.ChangePasswordRequest;
import com.iwas.auth.dto.UpdateProfileRequest;
import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.user.dto.CreateUserRequest;
import com.iwas.user.dto.UpdateUserRequest;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.service.UserService;
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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserMeResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(201).body(userService.createUser(request));
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<UserMeResponse> updateUser(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request, authenticatedUserResolver.currentUserRole()));
    }

    @GetMapping
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
