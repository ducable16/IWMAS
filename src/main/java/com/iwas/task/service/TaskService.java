package com.iwas.task.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.skill.entity.Skill;
import com.iwas.skill.repository.SkillRepository;
import com.iwas.task.dto.*;
import com.iwas.task.entity.Task;
import com.iwas.task.entity.TaskSkillRequirement;
import com.iwas.task.entity.TaskStatusHistory;
import com.iwas.task.repository.TaskRepository;
import com.iwas.task.repository.TaskSkillRequirementRepository;
import com.iwas.task.repository.TaskStatusHistoryRepository;
import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskSkillRequirementRepository taskSkillRequirementRepository;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    public List<TaskResponse> getTasksByProject(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        return toResponseList(tasks);
    }

    public List<TaskResponse> getMyTasks(Long userId) {
        List<Task> tasks = taskRepository.findByAssigneeId(userId);
        return toResponseList(tasks);
    }

    public TaskResponse getTaskById(Long id) {
        Task task = findTask(id);
        return toResponse(task, getSkillRequirements(task.getId()));
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request, Long reporterId) {
        Task task = new Task();
        task.setProjectId(request.getProjectId());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setType(request.getType());
        task.setPriority(request.getPriority());
        task.setEstimatedHours(request.getEstimatedHours());
        task.setStartDate(request.getStartDate());
        task.setDueDate(request.getDueDate());
        task.setAssigneeId(request.getAssigneeId());
        task.setReporterId(reporterId);
        task = taskRepository.save(task);

        if (request.getSkillRequirements() != null) {
            saveSkillRequirements(task.getId(), request.getSkillRequirements());
        }

        return toResponse(task, getSkillRequirements(task.getId()));
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findTask(id);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setType(request.getType());
        task.setPriority(request.getPriority());
        task.setEstimatedHours(request.getEstimatedHours());
        task.setStartDate(request.getStartDate());
        task.setDueDate(request.getDueDate());
        task.setAssigneeId(request.getAssigneeId());
        task = taskRepository.save(task);

        if (request.getSkillRequirements() != null) {
            taskSkillRequirementRepository.deleteByTaskId(id);
            saveSkillRequirements(task.getId(), request.getSkillRequirements());
        }

        return toResponse(task, getSkillRequirements(task.getId()));
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long id, TaskStatusUpdateRequest request, Long changedById) {
        Task task = findTask(id);
        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(id);
        history.setOldStatus(task.getStatus());
        history.setNewStatus(request.getStatus());
        history.setChangedBy(changedById);
        history.setNote(request.getNote());
        taskStatusHistoryRepository.save(history);

        task.setStatus(request.getStatus());
        if (request.getStatus().name().equals("DONE")) {
            task.setCompletedAt(LocalDateTime.now());
        }
        task = taskRepository.save(task);
        return toResponse(task, getSkillRequirements(task.getId()));
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findTask(id);
        task.setIsDeleted(true);
        taskRepository.save(task);
    }

    public List<TaskStatusHistoryResponse> getStatusHistory(Long taskId) {
        findTask(taskId);
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

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));
    }

    private void saveSkillRequirements(Long taskId, List<TaskSkillRequirementRequest> reqs) {
        reqs.forEach(req -> {
            TaskSkillRequirement tsr = new TaskSkillRequirement();
            tsr.setTaskId(taskId);
            tsr.setSkillId(req.getSkillId());
            tsr.setMinimumLevel(req.getMinimumLevel());
            tsr.setIsRequired(req.getIsRequired());
            taskSkillRequirementRepository.save(tsr);
        });
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

        Map<Long, List<TaskSkillRequirementResponse>> skillReqMap = tasks.stream()
                .collect(Collectors.toMap(Task::getId, t -> getSkillRequirements(t.getId())));

        List<Long> userIds = tasks.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getAssigneeId(), t.getReporterId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return tasks.stream()
                .map(t -> {
                    User assignee = userMap.get(t.getAssigneeId());
                    User reporter = userMap.get(t.getReporterId());
                    return toResponse(t, skillReqMap.getOrDefault(t.getId(), Collections.emptyList()), assignee, reporter);
                })
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
                .assignee(toUserMeResponse(assignee))
                .reporter(toUserMeResponse(reporter))
                .skillRequirements(skillReqs)
                .createdAt(t.getCreatedAt())
                .build();
    }

    private com.iwas.user.dto.UserMeResponse toUserMeResponse(User user) {
        if (user == null) return null;
        return com.iwas.user.dto.UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .build();
    }
}
