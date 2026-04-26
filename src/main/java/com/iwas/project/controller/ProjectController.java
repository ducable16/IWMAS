package com.iwas.project.controller;

import com.iwas.project.dto.*;
import com.iwas.project.enums.ProjectStatus;
import com.iwas.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> getAllProjects(
            @RequestParam(required = false) ProjectStatus status) {
        if (status != null) {
            return projectService.getProjectsByStatus(status);
        }
        return projectService.getAllProjects();
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

    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProjectRequest request) {
        return projectService.updateProject(id, request);
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public void delete(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @GetMapping("/{id}/members")
    public List<ProjectMemberResponse> getMembers(@PathVariable Long id) {
        return projectService.getProjectMembers(id);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(@PathVariable Long id,
                                           @Valid @RequestBody ProjectMemberRequest request) {
        return projectService.addMember(id, request);
    }

    @PostMapping("/{id}/members/{memberId}/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ProjectMemberResponse updateMember(@PathVariable Long id,
                                              @PathVariable Long memberId,
                                              @Valid @RequestBody ProjectMemberRequest request) {
        return projectService.updateMember(id, memberId, request);
    }

    @PostMapping("/{id}/members/{memberId}/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public void removeMember(@PathVariable Long id,
                             @PathVariable Long memberId) {
        projectService.removeMember(id, memberId);
    }
}
