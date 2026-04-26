package com.iwas.timelog.controller;

import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.timelog.dto.TimeLogRequest;
import com.iwas.timelog.dto.TimeLogResponse;
import com.iwas.timelog.service.TimeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-logs")
@RequiredArgsConstructor
public class TimeLogController {

    private final TimeLogService timeLogService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/my")
    public List<TimeLogResponse> getMyLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return timeLogService.getMyLogs(authenticatedUserResolver.currentUserId(), from, to);
    }

    @GetMapping("/task/{taskId}")
    public List<TimeLogResponse> getByTask(@PathVariable Long taskId) {
        return timeLogService.getLogsByTask(taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeLogResponse logTime(@Valid @RequestBody TimeLogRequest request) {
        return timeLogService.logTime(authenticatedUserResolver.currentUserId(), request);
    }

    @PostMapping("/{id}/update")
    public TimeLogResponse update(@PathVariable Long id,
                                  @Valid @RequestBody TimeLogRequest request) {
        return timeLogService.updateLog(authenticatedUserResolver.currentUserId(), id, request);
    }

    @PostMapping("/{id}/delete")
    public void delete(@PathVariable Long id) {
        timeLogService.deleteLog(authenticatedUserResolver.currentUserId(), id);
    }
}
