package com.iwas.project.controller;

import com.iwas.project.dto.*;
import com.iwas.project.service.ProjectService;
import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.skill.dto.RequiredSkill;
import com.iwas.user.dto.UserMeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectPageResponse getAllProjects(@ModelAttribute ProjectFilterRequest filter) {
        return projectService.searchProjects(filter);
    }

    @GetMapping("/my")
    public ProjectPageResponse getMyProjects(@ModelAttribute ProjectFilterRequest filter) {
        return projectService.searchMyProjects(authenticatedUserResolver.currentUserId(), filter);
    }

    @GetMapping("/suggest-code")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectCodeSuggestResponse suggestCode(@RequestParam String name) {
        return projectService.suggestCode(name);
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest request) {
        return projectService.createProject(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    @PatchMapping("/{id}/manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectResponse changeManager(@PathVariable Long id,
                                         @Valid @RequestBody ProjectManagerChangeRequest request) {
        return projectService.changeManager(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @GetMapping("/{id}/members")
    public List<ProjectMemberResponse> getMembers(@PathVariable Long id) {
        return projectService.getProjectMembers(id);
    }

    @GetMapping("/{id}/members/search")
    public List<UserMeResponse> searchMembers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(value = "requiredSkills", required = false) String requiredSkills,
            @RequestParam(defaultValue = "10") int size) {
        return projectService.searchProjectMembers(id, q, RequiredSkill.parse(requiredSkills), size);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(@PathVariable Long id,
                                           @Valid @RequestBody ProjectMemberRequest request) {
        return projectService.addMember(id, request);
    }

    @PutMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectMemberResponse updateMember(@PathVariable Long id,
                                              @PathVariable Long memberId,
                                              @Valid @RequestBody ProjectMemberRequest request) {
        return projectService.updateMember(id, memberId, request);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id,
                             @PathVariable Long memberId) {
        projectService.removeMember(id, memberId);
    }

    @GetMapping("/users/{userId}/effort-remaining")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public UserEffortRemainingResponse getUserEffortRemaining(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean detail) {
        return projectService.getUserEffortRemaining(userId, startDate, endDate, detail);
    }
}
