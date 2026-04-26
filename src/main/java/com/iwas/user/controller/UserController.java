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
import org.springframework.http.HttpStatus;
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
    public UserMeResponse getMe() {
        return userService.getMe(authenticatedUserResolver.currentUserId());
    }

    @PostMapping("/me/update")
    public UserMeResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authenticatedUserResolver.currentUserId(), request);
    }

    @PostMapping("/me/password")
    public String changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(authenticatedUserResolver.currentUserId(), request);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserMeResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public UserMeResponse updateUser(@PathVariable Long id,
                                     @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request, authenticatedUserResolver.currentUserRole());
    }

    @GetMapping
    public List<UserMeResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public UserMeResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserMeResponse activateUser(@PathVariable Long id) {
        return userService.toggleUserActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserMeResponse deactivateUser(@PathVariable Long id) {
        return userService.toggleUserActive(id, false);
    }
}
