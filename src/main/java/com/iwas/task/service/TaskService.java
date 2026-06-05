package com.iwas.task.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.notification.NotificationMessages;
import com.iwas.notification.enums.NotificationType;
import com.iwas.notification.service.NotificationService;
import com.iwas.project.service.ProjectService;
import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.skill.entity.EmployeeSkill;
import com.iwas.skill.entity.Skill;
import com.iwas.skill.enums.SkillLevel;
import com.iwas.skill.repository.EmployeeSkillRepository;
import com.iwas.skill.repository.SkillRepository;
import com.iwas.task.dto.*;
import com.iwas.task.entity.Task;
import com.iwas.task.entity.TaskSkillRequirement;
import com.iwas.task.entity.TaskStatusHistory;
import com.iwas.task.enums.TaskStatus;
import com.iwas.task.repository.TaskRepository;
import com.iwas.task.repository.TaskSkillRequirementRepository;
import com.iwas.task.repository.TaskSpecification;
import com.iwas.task.repository.TaskStatusHistoryRepository;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.entity.User;
import com.iwas.user.mapper.UserMapper;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskSkillRequirementRepository taskSkillRequirementRepository;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;
    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final UserMapper userMapper;

    @Lazy
    @Autowired
    private TaskCommentService taskCommentService;

    @Lazy
    @Autowired
    private TaskService self;

    public List<TaskResponse> getTasksByProject(Long projectId) {
        projectService.requireProjectAccess(projectId);
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return toResponseList(tasks);
    }

    public List<TaskResponse> getMyTasks(Long userId) {
        List<Task> tasks = taskRepository.findByAssigneeId(userId);
        return toResponseList(tasks);
    }

    public TaskPageResponse getTasksAssignedToUser(Long targetUserId, TaskFilterRequest filter) {
        userRepository.findById(targetUserId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        filter.setAssigneeId(targetUserId);
        return searchTasksForUser(filter);
    }

    public TaskPageResponse getTasksReportedByUser(Long targetUserId, TaskFilterRequest filter) {
        userRepository.findById(targetUserId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        filter.setReporterId(targetUserId);
        return searchTasksForUser(filter);
    }

    private TaskPageResponse searchTasksForUser(TaskFilterRequest filter) {
        int size = Math.min(filter.getSize(), 100);
        Sort sort = buildSort(filter.getSortBy(), filter.getSortDirection());
        PageRequest pageRequest = PageRequest.of(filter.getPage(), size, sort);

        Specification<Task> spec = TaskSpecification.fromFilter(filter);
        Specification<Task> filtered = applyAccessFilter(spec);
        if (filtered == null) {
            return TaskPageResponse.builder()
                    .content(List.of()).page(filter.getPage()).size(size)
                    .totalElements(0).totalPages(0).build();
        }

        Page<Task> page = taskRepository.findAll(filtered, pageRequest);
        List<TaskResponse> content = toResponseList(page.getContent());
        return TaskPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public TaskResponse getTaskById(Long id) {
        Task task = findTask(id);
        projectService.requireProjectAccess(task.getProjectId());
        return toDetailResponse(task);
    }

    public TaskPageResponse searchTasks(TaskFilterRequest filter) {
        if (filter.getProjectId() != null) {
            projectService.requireProjectAccess(filter.getProjectId());
        }

        int size = Math.min(filter.getSize(), 100);
        Sort sort = buildSort(filter.getSortBy(), filter.getSortDirection());
        PageRequest pageRequest = PageRequest.of(filter.getPage(), size, sort);

        Specification<Task> spec = TaskSpecification.fromFilter(filter);
        if (filter.getProjectId() == null) {
            Specification<Task> filtered = applyAccessFilter(spec);
            if (filtered == null) {
                return TaskPageResponse.builder()
                        .content(List.of()).page(filter.getPage()).size(size)
                        .totalElements(0).totalPages(0).build();
            }
            spec = filtered;
        }

        Page<Task> page = taskRepository.findAll(spec, pageRequest);
        List<TaskResponse> content = toResponseList(page.getContent());
        return TaskPageResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    // Access-check wrapper — delegates to the cached implementation via proxy
    public KanbanBoardResponse getKanbanBoard(Long projectId) {
        projectService.requireProjectAccess(projectId);
        return self.getKanbanBoardCached(projectId);
    }

    @Cacheable(value = "kanbanBoard", key = "#projectId")
    public KanbanBoardResponse getKanbanBoardCached(Long projectId) {
        List<Task> tasks = taskRepository.findAll(TaskSpecification.byProjectId(projectId));
        Map<TaskStatus, List<Task>> grouped = tasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus));

        List<KanbanColumnResponse> columns = Arrays.stream(TaskStatus.values())
                .map(status -> {
                    List<Task> columnTasks = grouped.getOrDefault(status, Collections.emptyList());
                    List<TaskResponse> taskResponses = toResponseList(columnTasks);
                    return KanbanColumnResponse.builder()
                            .status(status)
                            .displayName(status.getDisplayName())
                            .tasks(taskResponses)
                            .count(taskResponses.size())
                            .build();
                })
                .toList();

        return KanbanBoardResponse.builder()
                .projectId(projectId)
                .columns(columns)
                .build();
    }

    @Transactional
    @CacheEvict(value = "kanbanBoard", allEntries = true)
    public TaskResponse createTask(TaskRequest request, Long reporterId) {
        validateAssignee(request.getProjectId(), request.getAssigneeId());
        validateAssigneeSkills(request.getAssigneeId(), null, request.getSkillRequirements());
        Task task = new Task();
        applyRequest(task, request);
        task.setReporterId(reporterId);
        task = taskRepository.save(task);

        if (request.getSkillRequirements() != null) {
            saveSkillRequirements(task.getId(), request.getSkillRequirements());
        }

        if (task.getAssigneeId() != null) {
            notificationService.send(
                    task.getAssigneeId(), NotificationType.TASK_ASSIGNED,
                    NotificationMessages.newTaskAssigned(task.getTitle()),
                    "TASK", task.getId());
        }

        return toResponse(task, getSkillRequirements(task.getId()));
    }

    @Transactional
    @CacheEvict(value = "kanbanBoard", allEntries = true)
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTask(id);
        requireTaskEditAccess(task);
        validateAssignee(task.getProjectId(), task.getAssigneeId());
        Long oldAssigneeId = task.getAssigneeId();
        applyRequest(task, request);
        validateAssigneeSkills(task.getAssigneeId(), id, request.getSkillRequirements());
        task = taskRepository.save(task);

        if (request.getSkillRequirements() != null) {
            taskSkillRequirementRepository.deleteByTaskId(id);
            saveSkillRequirements(task.getId(), request.getSkillRequirements());
        }

        Long newAssigneeId = task.getAssigneeId();
        if (newAssigneeId != null && !newAssigneeId.equals(oldAssigneeId)) {
            notificationService.send(
                    newAssigneeId, NotificationType.TASK_ASSIGNED,
                    NotificationMessages.taskReassigned(task.getTitle()),
                    "TASK", task.getId());
        }

        return toResponse(task, getSkillRequirements(task.getId()));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "kanbanBoard", allEntries = true),
            @CacheEvict(value = "taskStatusHistory", key = "#id")
    })
    public TaskResponse updateTaskStatus(Long id, TaskStatusUpdateRequest request, Long changedById) {
        Task task = findTask(id);
        requireTaskEditAccess(task);

        if (!task.getStatus().canTransitionTo(request.getStatus())) {
            throw new AppException(ErrorCode.TASK_INVALID_STATUS_TRANSITION);
        }

        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(id);
        history.setOldStatus(task.getStatus());
        history.setNewStatus(request.getStatus());
        history.setChangedBy(changedById);
        history.setNote(request.getNote());
        taskStatusHistoryRepository.save(history);

        task.setStatus(request.getStatus());
        if (request.getStatus() == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (task.getCompletedAt() != null) {
            task.setCompletedAt(null);
        }
        task = taskRepository.save(task);

        NotificationMessages.NotificationContent statusMsg =
                NotificationMessages.taskStatusChanged(task.getTitle(), request.getStatus().getDisplayName());
        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(changedById)) {
            notificationService.send(task.getAssigneeId(), NotificationType.TASK_STATUS_CHANGED,
                    statusMsg, "TASK", id);
        }
        if (task.getReporterId() != null && !task.getReporterId().equals(changedById)
                && !task.getReporterId().equals(task.getAssigneeId())) {
            notificationService.send(task.getReporterId(), NotificationType.TASK_STATUS_CHANGED,
                    statusMsg, "TASK", id);
        }

        return toResponse(task, getSkillRequirements(task.getId()));
    }

    @Transactional
    @CacheEvict(value = "kanbanBoard", allEntries = true)
    public TaskResponse updateTaskDates(Long id, TaskDateUpdateRequest request) {
        Task task = findTask(id);
        requireTaskEditAccess(task);

        resolveAndApplyDates(task, request.getStartDate(), request.getDueDate());
        task = taskRepository.save(task);
        return toResponse(task, getSkillRequirements(task.getId()));
    }

    @Transactional
    @CacheEvict(value = "kanbanBoard", allEntries = true)
    public void deleteTask(Long id) {
        Task task = findTask(id);
        task.setIsDeleted(true);
        taskRepository.save(task);
    }

    // Access-check wrapper — delegates to the cached implementation via proxy
    public List<TaskStatusHistoryResponse> getStatusHistory(Long taskId) {
        Task task = findTask(taskId);
        projectService.requireProjectAccess(task.getProjectId());
        return self.getStatusHistoryCached(taskId);
    }

    @Cacheable(value = "taskStatusHistory", key = "#taskId")
    public List<TaskStatusHistoryResponse> getStatusHistoryCached(Long taskId) {
        return taskStatusHistoryRepository.findByTaskIdOrderByChangedAtAsc(taskId).stream()
                .map(h -> TaskStatusHistoryResponse.builder()
                        .id(h.getId())
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .changedBy(h.getChangedBy())
                        .note(h.getNote())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();
    }

    public List<CalendarDayResponse> getCalendarView(LocalDate from, LocalDate to, Long projectId) {
        if (projectId != null) {
            projectService.requireProjectAccess(projectId);
        }

        Specification<Task> spec = TaskSpecification.forCalendar(from, to, projectId);
        if (projectId == null) {
            Specification<Task> filtered = applyAccessFilter(spec);
            if (filtered == null) return Collections.emptyList();
            spec = filtered;
        }

        List<Task> tasks = taskRepository.findAll(spec);
        Map<LocalDate, List<Task>> grouped = tasks.stream()
                .collect(Collectors.groupingBy(Task::getDueDate));

        return from.datesUntil(to.plusDays(1))
                .filter(grouped::containsKey)
                .map(date -> {
                    List<TaskResponse> dayTasks = toResponseList(grouped.get(date));
                    return CalendarDayResponse.builder()
                            .date(date)
                            .tasks(dayTasks)
                            .count(dayTasks.size())
                            .build();
                })
                .toList();
    }

    // --- Validation helpers ---

    private void validateAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) return;
        if (!projectService.isProjectParticipant(projectId, assigneeId)) {
            throw new AppException(ErrorCode.TASK_ASSIGNEE_NOT_PROJECT_MEMBER);
        }
    }

    private void validateAssigneeSkills(Long assigneeId, Long taskId, List<TaskSkillRequirementRequest> incomingReqs) {
        if (assigneeId == null) return;

        if (incomingReqs != null) {
            for (TaskSkillRequirementRequest req : incomingReqs) {
                if (!Boolean.TRUE.equals(req.getIsRequired())) continue;
                checkAssigneeHasSkill(assigneeId, req.getSkillId(), req.getMinimumLevel());
            }
        } else if (taskId != null) {
            for (TaskSkillRequirement req : taskSkillRequirementRepository.findByTaskId(taskId)) {
                if (!Boolean.TRUE.equals(req.getIsRequired())) continue;
                checkAssigneeHasSkill(assigneeId, req.getSkillId(), req.getMinimumLevel());
            }
        }
    }

    private void checkAssigneeHasSkill(Long userId, Long skillId, SkillLevel minimumLevel) {
        employeeSkillRepository.findByUserIdAndSkillIdAndIsDeletedFalse(userId, skillId)
                .filter(es -> es.getLevel().ordinal() >= minimumLevel.ordinal())
                .orElseThrow(() -> new AppException(ErrorCode.TASK_ASSIGNEE_SKILL_NOT_MET));
    }

    // --- Access control helpers ---

    private void requireTaskEditAccess(Task task) {
        String role = authenticatedUserResolver.currentUserRole();
        if ("ADMIN".equals(role)) return;
        Long userId = authenticatedUserResolver.currentUserId();
        boolean isPM = projectService.isManagerOf(task.getProjectId(), userId);
        boolean isAssignee = userId.equals(task.getAssigneeId());
        if (!isPM && !isAssignee) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * Returns a spec restricted to the current user's accessible projects.
     * Returns null when the user has no accessible projects (empty result).
     * Returns the original spec unchanged when the user is ADMIN.
     */
    private Specification<Task> applyAccessFilter(Specification<Task> spec) {
        List<Long> accessibleIds = projectService.getAccessibleProjectIds();
        if (accessibleIds == null) return spec;
        if (accessibleIds.isEmpty()) return null;
        return spec.and((root, query, cb) -> root.get("projectId").in(accessibleIds));
    }

    // --- Private helpers ---

    private void applyRequest(Task task, TaskRequest request) {
        task.setProjectId(request.getProjectId());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setType(request.getType());
        task.setPriority(request.getPriority());
        task.setEstimatedHours(request.getEstimatedHours());
        task.setAssigneeId(request.getAssigneeId());
        task.setSprint(request.getSprint());
        task.setLabels(request.getLabels() != null ? request.getLabels() : new HashSet<>());
        task.setCustomFields(request.getCustomFields() != null ? request.getCustomFields() : new HashMap<>());
        resolveAndApplyDates(task, request.getStartDate(), request.getDueDate());
    }

    private static final int DEFAULT_HORIZON_WORKDAYS = 10;

    /**
     * Date resolution rules for workload model v2.1:
     *  - Both null              → reject (TASK_DATES_REQUIRED).
     *  - Only dueDate set       → startDate stays null (no planned start; the
     *                             arrangement engine projects one as output, and
     *                             the workload simulator treats null start as
     *                             "released today").
     *  - Only startDate set:
     *      • with estimate      → default dueDate = startDate + ceil(estimated) workdays − 1
     *                             (drip ~1h/day for open-ended tasks).
     *      • estimate null      → default dueDate = startDate + DEFAULT_HORIZON_WORKDAYS − 1
     *                             (placeholder; task contributes 0 to load until estimate is set
     *                             — counted as unestimatedTaskCount instead).
     *  - Both set               → validate start <= due, store as-is.
     */
    private void resolveAndApplyDates(Task task, LocalDate requestedStart, LocalDate requestedDue) {
        if (requestedStart == null && requestedDue == null) {
            throw new AppException(ErrorCode.TASK_DATES_REQUIRED);
        }

        LocalDate start = requestedStart; // optional — kept null when not provided
        LocalDate due = requestedDue;
        if (due == null) {
            // start is guaranteed non-null here (both-null already rejected above)
            int wd;
            BigDecimal estimated = task.getEstimatedHours();
            if (estimated != null && estimated.signum() > 0) {
                wd = Math.max(1, (int) Math.ceil(estimated.doubleValue()));
            } else {
                wd = DEFAULT_HORIZON_WORKDAYS;
            }
            due = addWorkdays(start, wd - 1);
        }

        if (start != null && start.isAfter(due)) {
            throw new AppException(ErrorCode.TASK_INVALID_DATE_RANGE);
        }

        task.setStartDate(start);
        task.setDueDate(due);
    }

    private static boolean isWorkday(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    private static LocalDate addWorkdays(LocalDate start, int workdaysToAdd) {
        if (workdaysToAdd <= 0) return start;
        LocalDate d = start;
        int added = 0;
        while (added < workdaysToAdd) {
            d = d.plusDays(1);
            if (isWorkday(d)) added++;
        }
        return d;
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
    }

    private void saveSkillRequirements(Long taskId, List<TaskSkillRequirementRequest> reqs) {
        List<TaskSkillRequirement> entities = reqs.stream().map(req -> {
            TaskSkillRequirement tsr = new TaskSkillRequirement();
            tsr.setTaskId(taskId);
            tsr.setSkillId(req.getSkillId());
            tsr.setMinimumLevel(req.getMinimumLevel());
            tsr.setIsRequired(req.getIsRequired());
            return tsr;
        }).toList();
        taskSkillRequirementRepository.saveAll(entities);
    }

    private List<TaskSkillRequirementResponse> getSkillRequirements(Long taskId) {
        List<TaskSkillRequirement> reqs = taskSkillRequirementRepository.findByTaskId(taskId);
        if (reqs.isEmpty()) return Collections.emptyList();

        Map<Long, String> skillNames = skillRepository.findAllById(
                reqs.stream().map(TaskSkillRequirement::getSkillId).toList()
        ).stream().collect(Collectors.toMap(Skill::getId, Skill::getName));

        return reqs.stream()
                .map(r -> TaskSkillRequirementResponse.builder()
                        .id(r.getId())
                        .skillId(r.getSkillId())
                        .skillName(skillNames.get(r.getSkillId()))
                        .minimumLevel(r.getMinimumLevel())
                        .isRequired(r.getIsRequired())
                        .build())
                .toList();
    }

    private List<TaskResponse> toResponseList(List<Task> tasks) {
        if (tasks.isEmpty()) return Collections.emptyList();

        List<Long> taskIds = tasks.stream().map(Task::getId).toList();

        List<TaskSkillRequirement> allReqs = taskSkillRequirementRepository.findByTaskIdIn(taskIds);
        Set<Long> skillIds = allReqs.stream().map(TaskSkillRequirement::getSkillId).collect(Collectors.toSet());
        Map<Long, String> skillNames = skillIds.isEmpty() ? Map.of() :
                skillRepository.findAllById(skillIds).stream()
                        .collect(Collectors.toMap(Skill::getId, Skill::getName));

        Map<Long, List<TaskSkillRequirementResponse>> skillReqMap = new HashMap<>();
        taskIds.forEach(id -> skillReqMap.put(id, new ArrayList<>()));
        allReqs.forEach(r -> skillReqMap.get(r.getTaskId()).add(
                TaskSkillRequirementResponse.builder()
                        .id(r.getId())
                        .skillId(r.getSkillId())
                        .skillName(skillNames.get(r.getSkillId()))
                        .minimumLevel(r.getMinimumLevel())
                        .isRequired(r.getIsRequired())
                        .build()
        ));

        List<Long> userIds = tasks.stream()
                .flatMap(t -> Stream.of(t.getAssigneeId(), t.getReporterId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return tasks.stream()
                .map(t -> toResponse(t, skillReqMap.getOrDefault(t.getId(), Collections.emptyList()),
                        userMap.get(t.getAssigneeId()), userMap.get(t.getReporterId())))
                .toList();
    }

    private TaskResponse toResponse(Task t, List<TaskSkillRequirementResponse> skillReqs) {
        User assignee = t.getAssigneeId() != null ? userRepository.findById(t.getAssigneeId()).orElse(null) : null;
        User reporter = t.getReporterId() != null ? userRepository.findById(t.getReporterId()).orElse(null) : null;
        return toResponse(t, skillReqs, assignee, reporter);
    }

    private TaskResponse toResponse(Task t, List<TaskSkillRequirementResponse> skillReqs, User assignee, User reporter) {
        return TaskResponse.builder()
                .id(t.getId())
                .projectId(t.getProjectId())
                .title(t.getTitle())
                .description(t.getDescription())
                .type(t.getType())
                .status(t.getStatus())
                .priority(t.getPriority())
                .estimatedHours(t.getEstimatedHours())
                .actualHours(t.getActualHours())
                .startDate(t.getStartDate())
                .dueDate(t.getDueDate())
                .completedAt(t.getCompletedAt())
                .sprint(t.getSprint())
                .labels(t.getLabels())
                .customFields(t.getCustomFields())
                .assignee(userMapper.toPublicView(assignee))
                .reporter(userMapper.toPublicView(reporter))
                .skillRequirements(skillReqs)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private TaskResponse toDetailResponse(Task t) {
        User assignee = t.getAssigneeId() != null ? userRepository.findById(t.getAssigneeId()).orElse(null) : null;
        User reporter = t.getReporterId() != null ? userRepository.findById(t.getReporterId()).orElse(null) : null;
        List<TaskCommentResponse> comments = taskCommentService.getCommentsByTask(t.getId());
        return TaskResponse.builder()
                .id(t.getId())
                .projectId(t.getProjectId())
                .title(t.getTitle())
                .description(t.getDescription())
                .type(t.getType())
                .status(t.getStatus())
                .priority(t.getPriority())
                .estimatedHours(t.getEstimatedHours())
                .actualHours(t.getActualHours())
                .startDate(t.getStartDate())
                .dueDate(t.getDueDate())
                .completedAt(t.getCompletedAt())
                .sprint(t.getSprint())
                .labels(t.getLabels())
                .customFields(t.getCustomFields())
                .assignee(userMapper.toPublicView(assignee))
                .reporter(userMapper.toPublicView(reporter))
                .skillRequirements(getSkillRequirements(t.getId()))
                .comments(comments)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private Sort buildSort(String sortBy, String direction) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "priority" -> "priority";
            case "duedate", "due_date" -> "dueDate";
            case "title" -> "title";
            case "updatedat", "updated_at" -> "updatedAt";
            case "startdate", "start_date" -> "startDate";
            default -> "createdAt";
        };
        return Sort.by(dir, field);
    }

    public List<UserMeResponse> getAssigneeCandidates(Long taskId, String q) {
        Task task = findTask(taskId);
        projectService.requireProjectAccess(task.getProjectId());

        Set<Long> participantIds = projectService.getExistingParticipantIds(task.getProjectId());
        if (participantIds.isEmpty()) return List.of();

        List<TaskSkillRequirement> required = taskSkillRequirementRepository.findByTaskId(taskId)
                .stream().filter(r -> Boolean.TRUE.equals(r.getIsRequired())).toList();

        Set<Long> qualified = new HashSet<>(participantIds);
        for (TaskSkillRequirement req : required) {
            List<SkillLevel> qualifying = Arrays.stream(SkillLevel.values())
                    .filter(l -> l.ordinal() >= req.getMinimumLevel().ordinal())
                    .toList();
            Set<Long> usersWithSkill = employeeSkillRepository
                    .findBySkillIdAndLevelIn(req.getSkillId(), qualifying)
                    .stream().map(EmployeeSkill::getUserId)
                    .collect(Collectors.toSet());
            qualified.retainAll(usersWithSkill);
            if (qualified.isEmpty()) return List.of();
        }

        String keyword = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return userRepository.searchByIdsAndKeyword(new ArrayList<>(qualified), keyword, PageRequest.of(0, 20))
                .stream().map(userMapper::toUserMeResponse).toList();
    }
}
