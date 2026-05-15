package com.iwas.workload.service;

import com.iwas.notification.service.NotificationService;
import com.iwas.project.entity.ProjectMember;
import com.iwas.project.repository.ProjectMemberRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.task.entity.Task;
import com.iwas.task.enums.TaskStatus;
import com.iwas.task.enums.TaskType;
import com.iwas.task.repository.TaskRepository;
import com.iwas.user.repository.UserRepository;
import com.iwas.workload.repository.BurnoutLogRepository;
import com.iwas.workload.repository.WorkloadSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkloadServiceTest {

    @Mock TaskRepository taskRepo;
    @Mock ProjectMemberRepository projectMemberRepo;
    @Mock ProjectRepository projectRepo;
    @Mock UserRepository userRepo;
    @Mock WorkloadSnapshotRepository workloadSnapshotRepo;
    @Mock BurnoutLogRepository burnoutLogRepo;
    @Mock NotificationService notificationService;

    @InjectMocks WorkloadService service;

    /** Pick a Monday on/after the given date so windows align to full workweeks. */
    private LocalDate nextMonday(LocalDate from) {
        return from.with(DayOfWeek.MONDAY).isBefore(from)
                ? from.with(DayOfWeek.MONDAY).plusWeeks(1)
                : from.with(DayOfWeek.MONDAY);
    }

    @Test
    void utilizationPerProject_nullWhenNoMemberRow() {
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        LocalDate mon = nextMonday(LocalDate.now().plusWeeks(2));
        BigDecimal result = service.utilizationPerProject(1L, 100L, mon, mon.plusDays(4));

        assertNull(result, "no member row → utilization is undefined");
    }

    @Test
    void utilizationPerProject_nullWhenAllocIsZero() {
        ProjectMember pm = new ProjectMember();
        pm.setAllocatedEffortPercent(0);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(pm));

        LocalDate mon = nextMonday(LocalDate.now().plusWeeks(2));
        BigDecimal result = service.utilizationPerProject(1L, 100L, mon, mon.plusDays(4));

        assertNull(result, "observer with 0% alloc → BLOCKED (null util)");
    }

    @Test
    void utilizationPerProject_nullWhenAllocIsNullLegacyRow() {
        ProjectMember pm = new ProjectMember();
        pm.setAllocatedEffortPercent(null);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(pm));

        LocalDate mon = nextMonday(LocalDate.now().plusWeeks(2));
        BigDecimal result = service.utilizationPerProject(1L, 100L, mon, mon.plusDays(4));

        assertNull(result, "legacy null alloc → treated like missing capacity");
    }

    @Test
    void utilizationPerProject_nullWhenWindowHasNoWorkdays() {
        LocalDate saturday = nextMonday(LocalDate.now().plusWeeks(2)).minusDays(2);
        LocalDate sunday = saturday.plusDays(1);

        BigDecimal result = service.utilizationPerProject(1L, 100L, saturday, sunday);

        assertNull(result, "weekend-only window has 0 workdays → null");
    }

    @Test
    void utilizationPerProject_zeroLoadWhenNoTasks() {
        ProjectMember pm = new ProjectMember();
        pm.setAllocatedEffortPercent(50);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(pm));
        when(taskRepo.findActiveTasksByAssigneeId(1L)).thenReturn(List.of());

        LocalDate mon = nextMonday(LocalDate.now().plusWeeks(2));
        BigDecimal result = service.utilizationPerProject(1L, 100L, mon, mon.plusDays(4));

        assertNotNull(result);
        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void utilizationPerProject_50PercentAllocDoublesUtilizationVs100Percent() {
        // Same task, same window — at 50% alloc, capacity is halved so util doubles.
        LocalDate mon = nextMonday(LocalDate.now().plusWeeks(2));
        LocalDate fri = mon.plusDays(4);

        Task t = new Task();
        t.setId(1L);
        t.setProjectId(100L);
        t.setAssigneeId(1L);
        t.setStartDate(mon);
        t.setDueDate(fri);
        t.setEstimatedHours(BigDecimal.valueOf(10));
        t.setActualHours(BigDecimal.ZERO);
        t.setStatus(TaskStatus.TODO);
        t.setType(TaskType.FEATURE);

        when(taskRepo.findActiveTasksByAssigneeId(1L)).thenReturn(List.of(t));

        ProjectMember full = new ProjectMember();
        full.setAllocatedEffortPercent(100);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(full));
        BigDecimal utilFull = service.utilizationPerProject(1L, 100L, mon, fri);

        ProjectMember half = new ProjectMember();
        half.setAllocatedEffortPercent(50);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(half));
        BigDecimal utilHalf = service.utilizationPerProject(1L, 100L, mon, fri);

        assertNotNull(utilFull);
        assertNotNull(utilHalf);
        // halfAlloc → halved capacity → doubled util (within 0.05% rounding)
        BigDecimal ratio = utilHalf.divide(utilFull, 4, java.math.RoundingMode.HALF_UP);
        assertTrue(ratio.subtract(BigDecimal.valueOf(2)).abs()
                        .compareTo(BigDecimal.valueOf(0.01)) < 0,
                "50% alloc should produce ~2x utilization vs 100%, got " + ratio);
    }
}
