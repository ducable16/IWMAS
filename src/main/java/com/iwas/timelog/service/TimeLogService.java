package com.iwas.timelog.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.task.entity.Task;
import com.iwas.task.repository.TaskRepository;
import com.iwas.timelog.dto.TimeLogRequest;
import com.iwas.timelog.dto.TimeLogResponse;
import com.iwas.timelog.entity.TimeLog;
import com.iwas.timelog.repository.TimeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final TaskRepository taskRepository;

    public List<TimeLogResponse> getMyLogs(Long userId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        return timeLogRepository.findByUserIdAndDateRange(userId, effectiveFrom, effectiveTo).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TimeLogResponse> getLogsByTask(Long taskId) {
        return timeLogRepository.findByTaskId(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TimeLogResponse logTime(Long userId, TimeLogRequest request) {
        Task task = taskRepository.findById(request.getTaskId())
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        timeLogRepository.findByTaskIdAndUserIdAndLogDateAndIsDeletedFalse(
                request.getTaskId(), userId, request.getLogDate())
                .ifPresent(tl -> { throw new AppException(ErrorCode.TIME_LOG_ALREADY_EXISTS); });

        TimeLog timeLog = new TimeLog();
        timeLog.setTaskId(request.getTaskId());
        timeLog.setUserId(userId);
        timeLog.setLogDate(request.getLogDate());
        timeLog.setHoursSpent(request.getHoursSpent());
        timeLog.setDescription(request.getDescription());
        TimeLog saved = timeLogRepository.save(timeLog);

        // Update actual hours on the task
        Double totalHours = timeLogRepository.sumHoursByTaskId(task.getId());
        if (totalHours != null) {
            task.setActualHours(BigDecimal.valueOf(totalHours));
            taskRepository.save(task);
        }

        return toResponse(saved);
    }

    @Transactional
    public TimeLogResponse updateLog(Long userId, Long logId, TimeLogRequest request) {
        TimeLog timeLog = timeLogRepository.findById(logId)
                .filter(tl -> tl.getUserId().equals(userId) && !Boolean.TRUE.equals(tl.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TIME_LOG_NOT_FOUND));

        timeLog.setHoursSpent(request.getHoursSpent());
        timeLog.setDescription(request.getDescription());
        TimeLog saved = timeLogRepository.save(timeLog);

        // Recalculate actual hours
        Double totalHours = timeLogRepository.sumHoursByTaskId(timeLog.getTaskId());
        taskRepository.findById(timeLog.getTaskId()).ifPresent(task -> {
            task.setActualHours(totalHours != null ? BigDecimal.valueOf(totalHours) : BigDecimal.ZERO);
            taskRepository.save(task);
        });

        return toResponse(saved);
    }

    @Transactional
    public void deleteLog(Long userId, Long logId) {
        TimeLog timeLog = timeLogRepository.findById(logId)
                .filter(tl -> tl.getUserId().equals(userId) && !Boolean.TRUE.equals(tl.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.TIME_LOG_NOT_FOUND));
        timeLog.setIsDeleted(true);
        timeLogRepository.save(timeLog);
    }

    private TimeLogResponse toResponse(TimeLog tl) {
        String taskTitle = taskRepository.findById(tl.getTaskId())
                .map(Task::getTitle).orElse(null);
        return TimeLogResponse.builder()
                .id(tl.getId())
                .taskId(tl.getTaskId())
                .taskTitle(taskTitle)
                .userId(tl.getUserId())
                .logDate(tl.getLogDate())
                .hoursSpent(tl.getHoursSpent())
                .description(tl.getDescription())
                .createdAt(tl.getCreatedAt())
                .build();
    }
}
