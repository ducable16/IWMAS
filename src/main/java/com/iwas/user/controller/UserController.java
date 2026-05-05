package com.iwas.user.controller;

import com.iwas.auth.dto.ChangePasswordRequest;
import com.iwas.auth.dto.UpdateProfileRequest;
import com.iwas.project.dto.ProjectFilterRequest;
import com.iwas.project.dto.ProjectPageResponse;
import com.iwas.project.service.ProjectService;
import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.task.dto.TaskFilterRequest;
import com.iwas.task.dto.TaskPageResponse;
import com.iwas.task.service.TaskService;
import com.iwas.user.dto.CreateUserRequest;
import com.iwas.user.dto.UpdateUserRequest;
import com.iwas.user.dto.UserFilterRequest;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.dto.UserPageResponse;
import com.iwas.user.enums.UserRole;
import com.iwas.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/me")
    public UserMeResponse getMe() {
        return userService.getMe(authenticatedUserResolver.currentUserId());
    }

    @PatchMapping("/me")
    public UserMeResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authenticatedUserResolver.currentUserId(), request);
    }

    @PatchMapping("/me/password")
    public String changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(authenticatedUserResolver.currentUserId(), request);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserMeResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public UserMeResponse updateUser(@PathVariable Long id,
                                     @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request, authenticatedUserResolver.currentUserRole());
    }

    @GetMapping
    public UserPageResponse getAllUsers(@ModelAttribute UserFilterRequest filter) {
        UserRole callerRole = UserRole.valueOf(authenticatedUserResolver.currentUserRole());
        return userService.getAllUsers(filter, callerRole);
    }

    @GetMapping("/{id}")
    public Object getUserById(@PathVariable Long id) {
        UserRole callerRole = UserRole.valueOf(authenticatedUserResolver.currentUserRole());
        return userService.getUserById(id, callerRole);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserMeResponse activateUser(@PathVariable Long id) {
        return userService.toggleUserActive(id, true);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserMeResponse deactivateUser(@PathVariable Long id) {
        return userService.toggleUserActive(id, false);
    }

    @GetMapping("/{userId}/projects")
    public ProjectPageResponse getUserProjects(
            @PathVariable Long userId,
            @ModelAttribute ProjectFilterRequest filter) {
        return projectService.getUserProjects(userId, filter);
    }

    @GetMapping("/{userId}/tasks/assigned")
    public TaskPageResponse getUserAssignedTasks(
            @PathVariable Long userId,
            @ModelAttribute TaskFilterRequest filter) {
        return taskService.getTasksAssignedToUser(userId, filter);
    }

    @GetMapping("/{userId}/tasks/reported")
    public TaskPageResponse getUserReportedTasks(
            @PathVariable Long userId,
            @ModelAttribute TaskFilterRequest filter) {
        return taskService.getTasksReportedByUser(userId, filter);
    }
}