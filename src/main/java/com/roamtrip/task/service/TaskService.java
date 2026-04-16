package com.roamtrip.task.service;

import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.common.exception.AppException;
import com.roamtrip.skill.entity.Skill;
import com.roamtrip.skill.repository.SkillRepository;
import com.roamtrip.task.dto.*;
import com.roamtrip.task.entity.Task;
import com.roamtrip.task.entity.TaskSkillRequirement;
import com.roamtrip.task.entity.TaskStatusHistory;
import com.roamtrip.task.repository.TaskRepository;
import com.roamtrip.task.repository.TaskSkillRequirementRepository;
import com.roamtrip.task.repository.TaskStatusHistoryRepository;
import com.roamtrip.user.entity.User;
import com.roamtrip.user.repository.UserRepository;
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

        Map<Long, String> assigneeNames = userRepository.findAllById(
                tasks.stream().filter(t -> t.getAssigneeId() != null)
                        .map(Task::getAssigneeId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        return tasks.stream()
                .map(t -> {
                    TaskResponse r = toResponse(t, skillReqMap.getOrDefault(t.getId(), Collections.emptyList()));
                    r.setAssigneeName(assigneeNames.get(t.getAssigneeId()));
                    return r;
                })
                .toList();
    }

    private TaskResponse toResponse(Task t, List<TaskSkillRequirementResponse> skillReqs) {
        String assigneeName = null;
        if (t.getAssigneeId() != null) {
            assigneeName = userRepository.findById(t.getAssigneeId())
                    .map(User::getFullName).orElse(null);
        }
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
                .assigneeId(t.getAssigneeId())
                .assigneeName(assigneeName)
                .reporterId(t.getReporterId())
                .skillRequirements(skillReqs)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
