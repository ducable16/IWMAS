package com.iwas.project.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.project.dto.*;
import com.iwas.project.entity.Project;
import com.iwas.project.entity.ProjectMember;
import com.iwas.project.repository.ProjectMemberRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.project.repository.ProjectSpecification;
import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public ProjectPageResponse searchProjects(ProjectFilterRequest filter) {
        int size = Math.min(filter.getSize(), 100);
        Sort sort = buildSort(filter.getSortBy(), filter.getSortDirection());
        PageRequest pageRequest = PageRequest.of(filter.getPage(), size, sort);

        Page<Project> page = projectRepository.findAll(ProjectSpecification.fromFilter(filter), pageRequest);

        List<ProjectResponse> content = page.getContent().stream()
                .map(this::toProjectResponse)
                .toList();
        return ProjectPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public ProjectPageResponse searchMyProjects(Long userId, ProjectFilterRequest filter) {
        List<Long> projectIds = projectMemberRepository.findActiveProjectsByUserId(userId)
                .stream().map(ProjectMember::getProjectId).toList();
        if (projectIds.isEmpty()) {
            return ProjectPageResponse.builder()
                    .content(List.of()).page(filter.getPage()).size(filter.getSize())
                    .totalElements(0).totalPages(0).build();
        }

        int size = Math.min(filter.getSize(), 100);
        Sort sort = buildSort(filter.getSortBy(), filter.getSortDirection());
        PageRequest pageRequest = PageRequest.of(filter.getPage(), size, sort);

        var spec = ProjectSpecification.fromFilter(filter)
                .and((root, query, cb) -> root.get("id").in(projectIds));
        Page<Project> page = projectRepository.findAll(spec, pageRequest);

        List<ProjectResponse> content = page.getContent().stream()
                .map(this::toProjectResponse)
                .toList();
        return ProjectPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
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

    private Sort buildSort(String sortBy, String direction) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "name" -> "name";
            case "status" -> "status";
            case "priority" -> "priority";
            case "startdate", "start_date" -> "startDate";
            case "enddate", "end_date" -> "endDate";
            case "updatedat", "updated_at" -> "updatedAt";
            default -> "createdAt";
        };
        return Sort.by(dir, field);
    }
}
