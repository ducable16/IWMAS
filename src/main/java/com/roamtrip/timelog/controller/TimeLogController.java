package com.roamtrip.timelog.controller;

import com.roamtrip.security.AuthenticatedUserResolver;
import com.roamtrip.timelog.dto.TimeLogRequest;
import com.roamtrip.timelog.dto.TimeLogResponse;
import com.roamtrip.timelog.service.TimeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<TimeLogResponse>> getMyLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(timeLogService.getMyLogs(authenticatedUserResolver.currentUserId(), from, to));
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TimeLogResponse>> getByTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(timeLogService.getLogsByTask(taskId));
    }

    @PostMapping
    public ResponseEntity<TimeLogResponse> logTime(@Valid @RequestBody TimeLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeLogService.logTime(authenticatedUserResolver.currentUserId(), request));
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<TimeLogResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody TimeLogRequest request) {
        return ResponseEntity.ok(timeLogService.updateLog(authenticatedUserResolver.currentUserId(), id, request));
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        timeLogService.deleteLog(authenticatedUserResolver.currentUserId(), id);
        return ResponseEntity.ok().build();
    }
}
