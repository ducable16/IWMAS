package com.iwas.workload.service;

import com.iwas.arrangement.config.AtcProperties;
import com.iwas.arrangement.core.TardinessArranger;
import com.iwas.arrangement.model.AtcConfig;
import com.iwas.notification.service.NotificationService;
import com.iwas.project.entity.Project;
import com.iwas.project.entity.ProjectMember;
import com.iwas.project.repository.ProjectMemberRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.task.entity.Task;
import com.iwas.task.enums.TaskPriority;
import com.iwas.task.enums.TaskStatus;
import com.iwas.task.enums.TaskType;
import com.iwas.task.repository.TaskRepository;
import com.iwas.user.entity.User;
import com.iwas.user.repository.UserRepository;
import com.iwas.workload.dto.CandidateWorkloadImpact;
import com.iwas.workload.dto.MemberWorkloadResponse;
import com.iwas.workload.enums.WorkloadLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkloadServiceTest {

    @Mock TaskRepository taskRepo;
    @Mock ProjectMemberRepository projectMemberRepo;
    @Mock ProjectRepository projectRepo;
    @Mock UserRepository userRepo;
    @Mock NotificationService notificationService;
    @Spy ScheduleSimulator scheduleSimulator = new ScheduleSimulator();
    @Spy TardinessArranger tardinessArranger = new TardinessArranger();

    @InjectMocks WorkloadService service;

    @BeforeAll
    static void initAtcConfig() {
        AtcConfig.initialize(new AtcProperties());
    }

    private static final long USER_ID = 1L;
    private static final long PROJECT_ID = 100L;

    private User user() {
        User u = new User();
        u.setId(USER_ID);
        u.setFullName("Test Member");
        u.setIsDeleted(false);
        return u;
    }

    private Project project() {
        Project p = new Project();
        p.setId(PROJECT_ID);
        p.setName("Test Project");
        p.setManagerId(999L);
        return p;
    }

    private ProjectMember membership(int alloc) {
        ProjectMember pm = new ProjectMember();
        pm.setProjectId(PROJECT_ID);
        pm.setUserId(USER_ID);
        pm.setAllocatedEffortPercent(alloc);
        return pm;
    }

    private Task task(long id, LocalDate start, LocalDate due, BigDecimal estimated) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(PROJECT_ID);
        t.setAssigneeId(USER_ID);
        t.setStartDate(start);
        t.setDueDate(due);
        t.setEstimatedHours(estimated);
        t.setStatus(TaskStatus.TODO);
        t.setType(TaskType.FEATURE);
        t.setPriority(TaskPriority.MEDIUM);
        return t;
    }

    private void stubMemberLookups(List<Task> activeTasks, ProjectMember pm) {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(taskRepo.findActiveTasksByAssigneeId(USER_ID)).thenReturn(activeTasks);
        when(projectMemberRepo.findActiveProjectsByUserId(USER_ID))
                .thenReturn(pm != null ? List.of(pm) : List.of());
        when(projectRepo.findByManagerId(USER_ID)).thenReturn(List.of());
        when(projectRepo.findAllById(any())).thenReturn(List.of(project()));
    }

    @Test
    void realtimeWorkloadIsAvailableWhenLightlyLoaded() {
        LocalDate today = LocalDate.now();
        Task light = task(1, today, ScheduleSimulator.addWorkdays(today, 9), BigDecimal.valueOf(8));
        stubMemberLookups(List.of(light), membership(100));

        MemberWorkloadResponse res = service.getUserWorkloadRealtime(USER_ID);

        assertEquals(WorkloadLevel.AVAILABLE, res.getWorkloadLevel());
        assertEquals(0, res.getOverdueTaskCount());
        assertEquals(0, res.getPredictedLateTaskCount());
        assertEquals(1, res.getActiveTaskCount());
    }

    @Test
    void realtimeWorkloadIsOverdueWhenATaskIsPastDue() {
        LocalDate today = LocalDate.now();
        Task overdue = task(1, today.minusDays(30), today.minusDays(15), BigDecimal.valueOf(8));
        stubMemberLookups(List.of(overdue), membership(100));

        MemberWorkloadResponse res = service.getUserWorkloadRealtime(USER_ID);

        assertEquals(WorkloadLevel.OVERDUE, res.getWorkloadLevel());
        assertEquals(1, res.getOverdueTaskCount());
    }

    @Test
    void reportedRemainingOverridesEstimateForLoad() {
        // Estimate 80h but member reported only 2h left → light load, not overloaded.
        LocalDate today = LocalDate.now();
        Task t = task(1, today, ScheduleSimulator.addWorkdays(today, 9), BigDecimal.valueOf(80));
        t.setReportedRemainingHours(BigDecimal.valueOf(2));
        t.setRemainingReportedDate(today);
        stubMemberLookups(List.of(t), membership(100));

        MemberWorkloadResponse res = service.getUserWorkloadRealtime(USER_ID);

        assertEquals(WorkloadLevel.AVAILABLE, res.getWorkloadLevel(),
                "reported 2h remaining should drive load, not the 80h estimate");
    }

    @Test
    void candidateTaskImpactIsUndefinedWithoutAllocationRow() {
        when(projectRepo.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(userRepo.existsById(USER_ID)).thenReturn(true);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.empty());

        CandidateWorkloadImpact impact = service.simulateWithCandidateTask(
                USER_ID, PROJECT_ID, BigDecimal.valueOf(8),
                LocalDate.now(), LocalDate.now().plusDays(7), TaskPriority.MEDIUM);

        assertEquals(WorkloadLevel.UNDEFINED, impact.getLevelAfter());
    }

    @Test
    void candidateTaskImpactDetectsAnIntroducedSlip() {
        LocalDate today = LocalDate.now();
        Task existing = task(1, today, ScheduleSimulator.addWorkdays(today, 1), BigDecimal.valueOf(8));
        when(projectRepo.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(userRepo.existsById(USER_ID)).thenReturn(true);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(membership(100)));
        when(taskRepo.findActiveTasksByProjectIdAndAssigneeId(PROJECT_ID, USER_ID))
                .thenReturn(List.of(existing));

        CandidateWorkloadImpact impact = service.simulateWithCandidateTask(
                USER_ID, PROJECT_ID, BigDecimal.valueOf(80),
                today, ScheduleSimulator.addWorkdays(today, 2), TaskPriority.MEDIUM);

        assertTrue(impact.isCandidateTaskWillSlip(), "an 80h task due in 2 days cannot fit");
        assertTrue(impact.isIntroducesNewSlip());
    }

    @Test
    void candidateTaskImpactIsCleanWhenItComfortablyFits() {
        LocalDate today = LocalDate.now();
        when(projectRepo.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(userRepo.existsById(USER_ID)).thenReturn(true);
        when(projectMemberRepo.findActiveMemberByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(membership(100)));
        when(taskRepo.findActiveTasksByProjectIdAndAssigneeId(PROJECT_ID, USER_ID))
                .thenReturn(List.of());

        CandidateWorkloadImpact impact = service.simulateWithCandidateTask(
                USER_ID, PROJECT_ID, BigDecimal.valueOf(8),
                today, ScheduleSimulator.addWorkdays(today, 9), TaskPriority.MEDIUM);

        assertFalse(impact.isCandidateTaskWillSlip());
        assertFalse(impact.isIntroducesNewSlip());
    }
}
