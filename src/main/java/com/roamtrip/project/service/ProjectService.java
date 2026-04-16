package com.roamtrip.project.service;

import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.common.exception.AppException;
import com.roamtrip.project.dto.*;
import com.roamtrip.project.entity.Project;
import com.roamtrip.project.entity.ProjectMember;
import com.roamtrip.project.enums.ProjectStatus;
import com.roamtrip.project.repository.ProjectMemberRepository;
import com.roamtrip.project.repository.ProjectRepository;
import com.roamtrip.user.entity.User;
import com.roamtrip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAllActive().stream()
                .map(this::toProjectResponse)
                .toList();
    }

    public List<ProjectResponse> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status).stream()
                .map(this::toProjectResponse)
                .toList();
    }

    public ProjectResponse getProjectById(Long id) {
        return toProjectResponse(findProject(id));
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        if (request.getCode() != null && !request.getCode().isBlank()) {
            projectRepository.findByCodeAndIsDeletedFalse(request.getCode())
                    .ifPresent(p -> { throw new AppException(ErrorCode.PROJECT_CODE_ALREADY_EXISTS); });
        }

        Project project = new Project();
        project.setName(request.getName().trim());
        project.setCode(request.getCode());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus());
        project.setPriority(request.getPriority());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setManagerId(request.getManagerId());
        return toProjectResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProject(id);
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus());
        project.setPriority(request.getPriority());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setManagerId(request.getManagerId());
        return toProjectResponse(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = findProject(id);
        project.setIsDeleted(true);
        projectRepository.save(project);
    }

    public List<ProjectMemberResponse> getProjectMembers(Long projectId) {
        findProject(projectId);
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        Map<Long, String> userNames = userRepository.findAllById(
                members.stream().map(ProjectMember::getUserId).toList()
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        return members.stream()
                .map(pm -> toMemberResponse(pm, userNames.get(pm.getUserId())))
                .toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(Long projectId, ProjectMemberRequest request) {
        findProject(projectId);
        projectMemberRepository.findByProjectIdAndUserIdAndIsDeletedFalse(projectId, request.getUserId())
                .ifPresent(pm -> { throw new AppException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS); });

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(request.getUserId());
        member.setRoleInProject(request.getRoleInProject());
        member.setAllocatedEffortPercent(request.getAllocatedEffortPercent());
        member.setJoinDate(request.getJoinDate());
        member.setNote(request.getNote());

        String userName = userRepository.findById(request.getUserId())
                .map(User::getFullName).orElse(null);
        return toMemberResponse(projectMemberRepository.save(member), userName);
    }

    @Transactional
    public ProjectMemberResponse updateMember(Long projectId, Long memberId, ProjectMemberRequest request) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .filter(pm -> pm.getProjectId().equals(projectId) && !Boolean.TRUE.equals(pm.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));

        member.setRoleInProject(request.getRoleInProject());
        member.setAllocatedEffortPercent(request.getAllocatedEffortPercent());
        member.setNote(request.getNote());

        String userName = userRepository.findById(member.getUserId())
                .map(User::getFullName).orElse(null);
        return toMemberResponse(projectMemberRepository.save(member), userName);
    }

    @Transactional
    public void removeMember(Long projectId, Long memberId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .filter(pm -> pm.getProjectId().equals(projectId) && !Boolean.TRUE.equals(pm.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
        member.setIsDeleted(true);
        projectMemberRepository.save(member);
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectResponse toProjectResponse(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .description(p.getDescription())
                .status(p.getStatus())
                .priority(p.getPriority())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .actualEndDate(p.getActualEndDate())
                .managerId(p.getManagerId())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember pm, String userFullName) {
        return ProjectMemberResponse.builder()
                .id(pm.getId())
                .projectId(pm.getProjectId())
                .userId(pm.getUserId())
                .userFullName(userFullName)
                .roleInProject(pm.getRoleInProject())
                .allocatedEffortPercent(pm.getAllocatedEffortPercent())
                .joinDate(pm.getJoinDate())
                .leaveDate(pm.getLeaveDate())
                .note(pm.getNote())
                .build();
    }
}
