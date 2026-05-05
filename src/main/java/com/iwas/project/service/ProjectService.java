package com.iwas.project.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.common.mesaging.event.ProjectIndexEvent;
import com.iwas.common.mesaging.publisher.ProjectIndexEventPublisher;
import com.iwas.project.dto.*;
import com.iwas.project.entity.Project;
import com.iwas.project.entity.ProjectMember;
import com.iwas.project.enums.ProjectStatus;
import com.iwas.project.repository.ProjectMemberRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.project.repository.ProjectSpecification;
import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.entity.User;
import com.iwas.user.mapper.UserMapper;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final ProjectIndexEventPublisher projectIndexEventPublisher;

    public ProjectPageResponse searchProjects(ProjectFilterRequest filter) {
        String role = authenticatedUserResolver.currentUserRole();
        if ("PROJECT_MANAGER".equals(role)) {
            // PM sees only projects they manage
            filter.setManagerId(authenticatedUserResolver.currentUserId());
        }

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

    public ProjectPageResponse getUserProjects(Long targetUserId, ProjectFilterRequest filter) {
        userRepository.findById(targetUserId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Set<Long> targetProjectIds = new HashSet<>();
        projectRepository.findByManagerId(targetUserId)
                .stream().map(Project::getId).forEach(targetProjectIds::add);
        projectMemberRepository.findActiveProjectsByUserId(targetUserId)
                .stream().map(ProjectMember::getProjectId).forEach(targetProjectIds::add);

        if (targetProjectIds.isEmpty()) {
            return ProjectPageResponse.builder()
                    .content(List.of()).page(filter.getPage()).size(filter.getSize())
                    .totalElements(0).totalPages(0).build();
        }

        List<Long> callerAccessibleIds = getAccessibleProjectIds();
        Set<Long> visibleIds;
        if (callerAccessibleIds == null) {
            visibleIds = targetProjectIds;
        } else {
            visibleIds = targetProjectIds.stream()
                    .filter(callerAccessibleIds::contains)
                    .collect(Collectors.toSet());
        }

        if (visibleIds.isEmpty()) {
            return ProjectPageResponse.builder()
                    .content(List.of()).page(filter.getPage()).size(filter.getSize())
                    .totalElements(0).totalPages(0).build();
        }

        int size = Math.min(filter.getSize(), 100);
        Sort sort = buildSort(filter.getSortBy(), filter.getSortDirection());
        PageRequest pageRequest = PageRequest.of(filter.getPage(), size, sort);

        var spec = ProjectSpecification.fromFilter(filter)
                .and((root, query, cb) -> root.get("id").in(visibleIds));
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
        Project project = findProject(id);
        requireProjectAccess(id);
        return toProjectResponse(project);
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
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setManagerId(request.getManagerId());
        Project saved = projectRepository.save(project);
        projectIndexEventPublisher.publish(toUpsertEvent(saved));
        return toProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProject(id);
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setManagerId(request.getManagerId());
        Project saved = projectRepository.save(project);
        projectIndexEventPublisher.publish(toUpsertEvent(saved));
        return toProjectResponse(saved);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = findProject(id);
        project.setIsDeleted(true);
        projectRepository.save(project);
        projectIndexEventPublisher.publish(ProjectIndexEvent.builder()
                .op(ProjectIndexEvent.Op.DELETE)
                .projectId(id)
                .build());
    }

    public List<ProjectMemberResponse> getProjectMembers(Long projectId) {
        findProject(projectId);
        requireProjectAccess(projectId);
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

        checkAllocationLimit(request.getUserId(), request.getAllocatedEffortPercent(), null);

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

        checkAllocationLimit(member.getUserId(), request.getAllocatedEffortPercent(), memberId);

        member.setRoleInProject(request.getRoleInProject());
        member.setAllocatedEffortPercent(request.getAllocatedEffortPercent());
        member.setNote(request.getNote());

        String userName = userRepository.findById(member.getUserId())
                .map(User::getFullName).orElse(null);
        return toMemberResponse(projectMemberRepository.save(member), userName);
    }

    /**
     * Acquires a row-level lock on the user row, then validates that adding
     * newAllocation% would not push the user's total active allocation above 100%.
     * The lock serializes concurrent allocation changes for the same user within
     * the surrounding @Transactional boundary.
     */
    private void checkAllocationLimit(Long userId, Integer newAllocation, Long excludeMemberId) {
        if (newAllocation == null || newAllocation == 0) return;

        userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Long current = excludeMemberId == null
                ? projectMemberRepository.sumActiveAllocationByUserId(userId)
                : projectMemberRepository.sumActiveAllocationExcluding(userId, excludeMemberId);

        long currentTotal = current == null ? 0L : current;
        if (currentTotal + newAllocation > 100) {
            throw new AppException(ErrorCode.USER_OVER_ALLOCATED);
        }
    }

    @Transactional
    public void removeMember(Long projectId, Long memberId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .filter(pm -> pm.getProjectId().equals(projectId) && !Boolean.TRUE.equals(pm.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
        member.setIsDeleted(true);
        projectMemberRepository.save(member);
    }

    // --- Search index helpers ---

    private ProjectIndexEvent toUpsertEvent(Project p) {
        return ProjectIndexEvent.builder()
                .op(ProjectIndexEvent.Op.UPSERT)
                .projectId(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .status(p.getStatus() == null ? null : p.getStatus().name())
                .managerId(p.getManagerId())
                .build();
    }

    // --- Member search ---

    public List<UserMeResponse> searchProjectMembers(Long projectId, String q, int size) {
        Project project = findProject(projectId);
        requireProjectAccess(projectId);

        Set<Long> participantIds = new HashSet<>();
        participantIds.add(project.getManagerId());
        projectMemberRepository.findByProjectId(projectId)
                .stream().map(ProjectMember::getUserId)
                .forEach(participantIds::add);

        if (participantIds.isEmpty()) return List.of();

        String keyword = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        int limit = Math.min(size, 20);
        List<User> users = userRepository.searchByIdsAndKeyword(
                new ArrayList<>(participantIds), keyword, PageRequest.of(0, limit));

        return users.stream().map(UserMapper::toUserMeResponse).toList();
    }

    // --- Access control helpers ---

    public boolean isManagerOf(Long projectId, Long userId) {
        return projectRepository.findById(projectId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(p -> userId.equals(p.getManagerId()))
                .orElse(false);
    }

    public boolean isMemberOf(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserIdAndIsDeletedFalse(projectId, userId).isPresent();
    }

    public boolean isProjectParticipant(Long projectId, Long userId) {
        return isManagerOf(projectId, userId) || isMemberOf(projectId, userId);
    }

    public void requireProjectAccess(Long projectId) {
        String role = authenticatedUserResolver.currentUserRole();
        if ("ADMIN".equals(role)) return;
        Long userId = authenticatedUserResolver.currentUserId();
        if (!isManagerOf(projectId, userId) && !isMemberOf(projectId, userId)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * Returns the IDs of projects the current user may access.
     * Returns null for ADMIN (no filter needed — all projects accessible).
     */
    public List<Long> getAccessibleProjectIds() {
        String role = authenticatedUserResolver.currentUserRole();
        if ("ADMIN".equals(role)) return null;
        Long userId = authenticatedUserResolver.currentUserId();
        List<Long> memberIds = projectMemberRepository.findActiveProjectsByUserId(userId)
                .stream().map(ProjectMember::getProjectId).toList();
        if ("PROJECT_MANAGER".equals(role)) {
            List<Long> managedIds = projectRepository.findByManagerId(userId)
                    .stream().map(Project::getId).toList();
            return Stream.concat(memberIds.stream(), managedIds.stream()).distinct().toList();
        }
        return memberIds;
    }

    // --- Effort remaining ---

    public UserEffortRemainingResponse getUserEffortRemaining(Long userId, LocalDate startDate,
                                                               LocalDate endDate, boolean detail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<ProjectMember> activeMemberships = projectMemberRepository.findActiveProjectsByUserId(userId);
        if (activeMemberships.isEmpty()) {
            return emptyEffortResponse(userId, user.getFullName(), startDate, endDate, detail);
        }

        Map<Long, Project> projectMap = projectRepository
                .findAllById(activeMemberships.stream().map(ProjectMember::getProjectId).toList())
                .stream().collect(Collectors.toMap(Project::getId, p -> p));

        List<ProjectMember> relevant = activeMemberships.stream()
                .filter(pm -> {
                    Project p = projectMap.get(pm.getProjectId());
                    if (p == null) return false;
                    if (p.getStatus() == ProjectStatus.COMPLETED || p.getStatus() == ProjectStatus.CANCELLED) return false;
                    if (startDate == null && endDate == null) return true;
                    return periodsOverlap(p.getStartDate(), p.getEndDate(), startDate, endDate);
                })
                .toList();

        List<AllocationInterval> intervals = relevant.stream()
                .filter(pm -> pm.getAllocatedEffortPercent() != null && pm.getAllocatedEffortPercent() > 0)
                .map(pm -> {
                    Project p = projectMap.get(pm.getProjectId());
                    LocalDate s = p.getStartDate() != null ? p.getStartDate() : LocalDate.of(2000, 1, 1);
                    LocalDate e = p.getEndDate()   != null ? p.getEndDate()   : LocalDate.of(9999, 12, 31);
                    return new AllocationInterval(s, e, pm.getAllocatedEffortPercent(),
                            p.getId(), p.getName(), p.getCode());
                })
                .toList();

        SweepResult sweep = buildSweepResult(intervals, detail);

        List<UserEffortRemainingResponse.AllocationEntry> entries = relevant.stream()
                .map(pm -> {
                    Project p = projectMap.get(pm.getProjectId());
                    return UserEffortRemainingResponse.AllocationEntry.builder()
                            .projectId(p.getId())
                            .projectName(p.getName())
                            .projectCode(p.getCode())
                            .allocatedPercent(pm.getAllocatedEffortPercent() != null ? pm.getAllocatedEffortPercent() : 0)
                            .projectStartDate(p.getStartDate())
                            .projectEndDate(p.getEndDate())
                            .build();
                })
                .toList();

        return UserEffortRemainingResponse.builder()
                .userId(userId)
                .userName(user.getFullName())
                .queryStart(startDate)
                .queryEnd(endDate)
                .peakAllocatedPercent(sweep.peak())
                .remainingPercent(Math.max(0, 100 - sweep.peak()))
                .overlappingAllocations(entries)
                .futureAvailabilityNotes(sweep.futureNotes())
                .allocationTimeline(sweep.timeline())
                .build();
    }

    private UserEffortRemainingResponse emptyEffortResponse(Long userId, String userName,
                                                             LocalDate startDate, LocalDate endDate,
                                                             boolean detail) {
        return UserEffortRemainingResponse.builder()
                .userId(userId).userName(userName)
                .queryStart(startDate).queryEnd(endDate)
                .peakAllocatedPercent(0).remainingPercent(100)
                .overlappingAllocations(List.of())
                .futureAvailabilityNotes(List.of())
                .allocationTimeline(detail ? List.of() : null)
                .build();
    }

    /**
     * Two date ranges overlap when neither ends strictly before the other starts.
     * Null means open-ended (no bound in that direction).
     */
    private boolean periodsOverlap(LocalDate s1, LocalDate e1, LocalDate s2, LocalDate e2) {
        boolean s1AfterE2 = s1 != null && e2 != null && s1.isAfter(e2);
        boolean s2AfterE1 = s2 != null && e1 != null && s2.isAfter(e1);
        return !s1AfterE2 && !s2AfterE1;
    }

    /**
     * Sweep-line over allocation intervals.
     * Always builds futureAvailabilityNotes (lightweight).
     * Builds allocationTimeline only when detail=true.
     */
    private SweepResult buildSweepResult(List<AllocationInterval> intervals, boolean detail) {
        if (intervals.isEmpty()) {
            return new SweepResult(0, List.of(), detail ? List.of() : null);
        }

        LocalDate today = LocalDate.now();
        TreeMap<LocalDate, Integer> eventDeltas = new TreeMap<>();
        Map<LocalDate, List<AllocationInterval>> startsByDate = new HashMap<>();
        Map<LocalDate, List<AllocationInterval>> endsByDate = new HashMap<>();

        for (AllocationInterval i : intervals) {
            eventDeltas.merge(i.start(), i.percent(), Integer::sum);
            eventDeltas.merge(i.end().plusDays(1), -i.percent(), Integer::sum);
            startsByDate.computeIfAbsent(i.start(), k -> new ArrayList<>()).add(i);
            endsByDate.computeIfAbsent(i.end().plusDays(1), k -> new ArrayList<>()).add(i);
        }

        int peak = 0, running = 0;
        List<UserEffortRemainingResponse.FutureAvailabilityNote> futureNotes = new ArrayList<>();
        List<UserEffortRemainingResponse.TimelineSegment> timeline = detail ? new ArrayList<>() : null;
        List<LocalDate> sortedDates = new ArrayList<>(eventDeltas.keySet());

        for (int idx = 0; idx < sortedDates.size(); idx++) {
            LocalDate date = sortedDates.get(idx);
            int delta = eventDeltas.get(date);
            running += delta;
            peak = Math.max(peak, running);

            // Note: capacity freed in the future
            if (delta < 0 && date.isAfter(today)) {
                List<AllocationInterval> ending = endsByDate.getOrDefault(date, List.of());
                futureNotes.add(UserEffortRemainingResponse.FutureAvailabilityNote.builder()
                        .availableFrom(date)
                        .additionalFreePercent(-delta)
                        .cumulativeRemainingPercent(Math.max(0, 100 - running))
                        .triggeringProjects(ending.stream().map(this::toProjectRef).toList())
                        .build());
            }

            // Timeline segment: from this event date to the day before the next
            if (detail) {
                LocalDate segEnd = idx + 1 < sortedDates.size()
                        ? sortedDates.get(idx + 1).minusDays(1)
                        : null;
                List<AllocationInterval> starting = startsByDate.getOrDefault(date, List.of());
                List<AllocationInterval> ending = endsByDate.getOrDefault(date, List.of());
                timeline.add(UserEffortRemainingResponse.TimelineSegment.builder()
                        .from(date)
                        .to(segEnd)
                        .allocatedPercent(running)
                        .remainingPercent(Math.max(0, 100 - running))
                        .changeSummary(UserEffortRemainingResponse.ChangeSummary.builder()
                                .deltaPercent(delta)
                                .startingProjects(starting.isEmpty() ? null : starting.stream().map(this::toProjectRef).toList())
                                .endingProjects(ending.isEmpty() ? null : ending.stream().map(this::toProjectRef).toList())
                                .build())
                        .build());
            }
        }

        return new SweepResult(peak, futureNotes, timeline);
    }

    private UserEffortRemainingResponse.ProjectRef toProjectRef(AllocationInterval ai) {
        return UserEffortRemainingResponse.ProjectRef.builder()
                .projectId(ai.projectId())
                .projectName(ai.projectName())
                .projectCode(ai.projectCode())
                .allocatedPercent(ai.percent())
                .build();
    }

    private record AllocationInterval(LocalDate start, LocalDate end, int percent,
                                       Long projectId, String projectName, String projectCode) {}

    private record SweepResult(int peak,
                                List<UserEffortRemainingResponse.FutureAvailabilityNote> futureNotes,
                                List<UserEffortRemainingResponse.TimelineSegment> timeline) {}

    // --- Private helpers ---

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
