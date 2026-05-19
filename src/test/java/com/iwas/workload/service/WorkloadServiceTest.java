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

        Task t = makeTask(mon, fri, BigDecimal.valueOf(10), BigDecimal.ZERO);
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

    @Test
    void utilizationPerProject_actualHoursDoNotAffectLoad() {
        // Virtual burn model derives load from schedule alone — actualHours is
        // reporting-only and must not change utilization.
        LocalDate mon = nextMonday(LocalDate.now().plusWeeks(2));
        LocalDate fri = mon.plusDays(4);

        ProjectMember pm = new ProjectMember();
        pm.setAllocatedEffortPercent(100);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(pm));

        Task zeroLogged = makeTask(mon, fri, BigDecimal.valueOf(10), BigDecimal.ZERO);
        when(taskRepo.findActiveTasksByAssigneeId(1L)).thenReturn(List.of(zeroLogged));
        BigDecimal utilNoLog = service.utilizationPerProject(1L, 100L, mon, fri);

        Task heavilyLogged = makeTask(mon, fri, BigDecimal.valueOf(10), BigDecimal.valueOf(8));
        when(taskRepo.findActiveTasksByAssigneeId(1L)).thenReturn(List.of(heavilyLogged));
        BigDecimal utilWithLog = service.utilizationPerProject(1L, 100L, mon, fri);

        assertNotNull(utilNoLog);
        assertNotNull(utilWithLog);
        assertEquals(0, utilNoLog.compareTo(utilWithLog),
                "actualHours must not change workload util; got " + utilNoLog + " vs " + utilWithLog);
    }

    @Test
    void utilizationPerProject_overdueTaskCollapsesFullEstimatedToToday() {
        // Overdue task should still surface in workload (bug P2 of v1 model).
        // Collapse to today with full estimated load.
        LocalDate today = LocalDate.now();
        // Build a window that includes today but starts on a Monday to keep workday math stable.
        LocalDate mon = today.with(DayOfWeek.MONDAY);
        LocalDate fri = mon.plusDays(4);
        if (today.isBefore(mon) || today.isAfter(fri)) {
            // Skip — we'd need a window that always covers today, but on a weekend today won't
            // be in [mon, fri]. Fall back to next Monday-Friday window (overdue still applies
            // because dueDate is well in the past).
            mon = nextMonday(today.plusWeeks(1));
            fri = mon.plusDays(4);
        }

        ProjectMember pm = new ProjectMember();
        pm.setAllocatedEffortPercent(100);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(pm));

        Task overdue = makeTask(today.minusDays(20), today.minusDays(10),
                BigDecimal.valueOf(6), BigDecimal.ZERO);
        when(taskRepo.findActiveTasksByAssigneeId(1L)).thenReturn(List.of(overdue));

        BigDecimal util = service.utilizationPerProject(1L, 100L, mon, fri);
        assertNotNull(util, "overdue task must contribute to util, not return null");
        assertTrue(util.compareTo(BigDecimal.ZERO) > 0,
                "overdue task should contribute positive load; got " + util);
    }

    @Test
    void utilizationPerProject_nullEstimateContributesZeroLoad() {
        // v2.1 chốt: task with no usable estimate must NOT be silently filled by
        // typeDefault — it contributes 0 to load and is surfaced separately via
        // unestimatedTaskCount (verified in DTO-level tests).
        LocalDate mon = nextMonday(LocalDate.now().plusWeeks(2));
        LocalDate fri = mon.plusDays(4);

        ProjectMember pm = new ProjectMember();
        pm.setAllocatedEffortPercent(100);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(pm));

        Task unestimated = makeTask(mon, fri, null, BigDecimal.ZERO);
        when(taskRepo.findActiveTasksByAssigneeId(1L)).thenReturn(List.of(unestimated));

        BigDecimal util = service.utilizationPerProject(1L, 100L, mon, fri);
        assertNotNull(util);
        assertEquals(0, util.compareTo(BigDecimal.ZERO),
                "task with null estimate must not contribute to load; got " + util);
    }

    @Test
    void utilizationPerProject_zeroOrNegativeEstimateContributesZeroLoad() {
        // Defensive: estimated = 0 (degenerate data) must behave like null
        // — task counts toward unestimatedTaskCount, not load.
        LocalDate mon = nextMonday(LocalDate.now().plusWeeks(2));
        LocalDate fri = mon.plusDays(4);

        ProjectMember pm = new ProjectMember();
        pm.setAllocatedEffortPercent(100);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(pm));

        Task zeroEst = makeTask(mon, fri, BigDecimal.ZERO, BigDecimal.ZERO);
        when(taskRepo.findActiveTasksByAssigneeId(1L)).thenReturn(List.of(zeroEst));

        BigDecimal util = service.utilizationPerProject(1L, 100L, mon, fri);
        assertNotNull(util);
        assertEquals(0, util.compareTo(BigDecimal.ZERO),
                "task with zero estimate must not contribute to load; got " + util);
    }

    private Task makeTask(LocalDate start, LocalDate due,
                          BigDecimal estimated, BigDecimal actual) {
        Task t = new Task();
        t.setId(1L);
        t.setProjectId(100L);
        t.setAssigneeId(1L);
        t.setStartDate(start);
        t.setDueDate(due);
        t.setEstimatedHours(estimated);
        t.setActualHours(actual);
        t.setStatus(TaskStatus.TODO);
        t.setType(TaskType.FEATURE);
        return t;
    }
}
