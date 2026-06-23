package com.iwas.arrangement.service;

import com.iwas.arrangement.core.AtcDispatcher;
import com.iwas.arrangement.core.AtcIndex;
import com.iwas.arrangement.core.AtcTaskMapper;
import com.iwas.arrangement.core.TardinessArranger;
import com.iwas.arrangement.dto.ArrangeResponse;
import com.iwas.arrangement.dto.NextTaskResponse;
import com.iwas.arrangement.model.ArrangedTask;
import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.project.entity.Project;
import com.iwas.project.entity.ProjectMember;
import com.iwas.project.repository.ProjectMemberRepository;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.task.entity.Task;
import com.iwas.task.enums.TaskPriority;
import com.iwas.task.repository.TaskRepository;
import com.iwas.workload.service.ScheduleSimulator;
import com.iwas.workload.service.ScheduleSimulator.LaneSimulation;
import com.iwas.workload.service.ScheduleSimulator.ScheduledTask;
import com.iwas.workload.service.ScheduleSimulator.TaskSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the ATC task-arrangement use cases for a single member lane:
 * loads the lane, reduces it to {@link AtcTask}s, runs the heuristic and
 * enriches the result with calendar projections from the workload simulator.
 *
 * <p>Output is advisory and never persisted (the suggested order does not touch
 * {@code executionSeq}); a member applies it through the existing
 * schedule-save endpoint if they want.
 */
@Service
@RequiredArgsConstructor
public class TaskArrangementService {

    private static final double DAILY_HOURS = 8.0;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final ScheduleSimulator scheduleSimulator;
    private final TardinessArranger arranger;
    private final AtcDispatcher dispatcher;

    /** Static arrangement — suggested execution order for the lane. */
    @Transactional(readOnly = true)
    public ArrangeResponse arrangeLane(Long projectId, Long assigneeId,
                                       Double kOverride, Map<TaskPriority, Double> weightOverrides) {
        Lane lane = loadLane(projectId, assigneeId);
        AtcConfig config = AtcConfig.getDefault().withOverrides(kOverride, weightOverrides);
        LocalDate today = LocalDate.now();
        double orderingCap = lane.dailyCap() > 0 ? lane.dailyCap() : DAILY_HOURS;

        List<AtcTask> atcTasks = lane.workable().stream()
                .map(t -> AtcTaskMapper.from(t, today, orderingCap))
                .toList();
        List<ArrangedTask> arranged = arranger.arrange(atcTasks, config);

        Map<Long, Task> byId = lane.workable().stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        List<ScheduledTask> orderedForSim = arranged.stream()
                .map(a -> toScheduledTask(byId.get(a.taskId())))
                .toList();
        Map<Long, TaskSchedule> calendar = projectCalendar(lane.dailyCap(), orderedForSim, today);

        List<ArrangeResponse.Item> items = new ArrayList<>();
        for (ArrangedTask a : arranged) {
            Task task = byId.get(a.taskId());
            TaskSchedule sched = calendar.get(a.taskId());
            items.add(new ArrangeResponse.Item(
                    a.taskId(),
                    task.getTitle(),
                    a.position(),
                    task.getPriority(),
                    a.slackHours(),
                    sched != null ? sched.projectedStart() : null,
                    sched != null ? sched.projectedFinish() : null,
                    a.projectedTardinessHours(),
                    sched != null ? sched.lateByWorkdays() : 0,
                    sched != null && sched.willSlip(),
                    a.estimateDefaulted(),
                    a.reason()));
        }

        return new ArrangeResponse(projectId, assigneeId, lane.alloc(),
                capacityBd(lane.dailyCap()), config.k(), items);
    }

    /** Online dispatch — the task the member should pick up next, right now. */
    @Transactional(readOnly = true)
    public NextTaskResponse nextTask(Long projectId, Long assigneeId,
                                     Double kOverride, Map<TaskPriority, Double> weightOverrides) {
        Lane lane = loadLane(projectId, assigneeId);
        if (lane.workable().isEmpty()) return NextTaskResponse.empty(projectId, assigneeId);

        AtcConfig config = AtcConfig.getDefault().withOverrides(kOverride, weightOverrides);
        LocalDate today = LocalDate.now();
        double orderingCap = lane.dailyCap() > 0 ? lane.dailyCap() : DAILY_HOURS;

        List<AtcTask> eligible = lane.workable().stream()
                .map(t -> AtcTaskMapper.from(t, today, orderingCap))
                .toList();
        Optional<AtcTask> next = dispatcher.nextTask(eligible, config);
        if (next.isEmpty()) return NextTaskResponse.empty(projectId, assigneeId);

        long nextId = next.get().id();
        Task nextTask = null;
        for(Task t : lane.workable()) {
            if (t.getId().equals(nextId)) {
                nextTask = t;
            }
        }
        if(nextTask == null) throw new NoSuchElementException("No value present");
        return new NextTaskResponse(projectId, assigneeId, false,
                nextTask.getId(), nextTask.getTitle(), nextTask.getPriority(),
                reasonFor(nextTask, today, orderingCap, config));
    }

    // ─── lane loading ─────────────────────────────────────────────────────────

    private record Lane(Integer alloc, double dailyCap, List<Task> workable) {
    }

    private Lane loadLane(Long projectId, Long assigneeId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        ProjectMember pmRow = projectMemberRepository
                .findActiveMemberByProjectIdAndUserId(projectId, assigneeId).orElse(null);
        boolean managerOnly = pmRow == null && assigneeId.equals(project.getManagerId());
        if (pmRow == null && !managerOnly) throw new AppException(ErrorCode.FORBIDDEN);

        Integer alloc = pmRow != null ? pmRow.getAllocatedEffortPercent() : null;
        List<Task> laneTasks = taskRepository
                .findActiveTasksByProjectIdAndAssigneeId(projectId, assigneeId);
        List<Task> workable = laneTasks.stream().filter(TaskArrangementService::isWorkable).toList();
        return new Lane(alloc, dailyCapHours(alloc), workable);
    }

    /** Runs the workload simulator over the ATC-ordered tasks to attach calendar dates. */
    private Map<Long, TaskSchedule> projectCalendar(double dailyCap,
                                                    List<ScheduledTask> ordered,
                                                    LocalDate today) {
        if (dailyCap <= 0 || ordered.isEmpty()) return Map.of();
        LaneSimulation sim = scheduleSimulator.simulate(ordered, capacityBd(dailyCap), today);
        return sim.schedules().stream()
                .collect(Collectors.toMap(TaskSchedule::taskId, ts -> ts));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static ScheduledTask toScheduledTask(Task t) {
        return new ScheduledTask(t.getId(), t.getProjectId(), t.getEstimatedHours(),
                t.getStartDate(), t.getDueDate(), t.getPriority());
    }

    private static boolean isWorkable(Task t) {
        BigDecimal est = t.getEstimatedHours();
        return est != null && est.signum() > 0;
    }

    /** Daily lane capacity in hours: 8h × allocation%; 0 when there is no allocation. */
    private static double dailyCapHours(Integer alloc) {
        if (alloc == null || alloc <= 0) return 0.0;
        return DAILY_HOURS * alloc / 100.0;
    }

    private static BigDecimal capacityBd(double dailyCap) {
        if (dailyCap <= 0) return null;
        return BigDecimal.valueOf(dailyCap).setScale(2, RoundingMode.HALF_UP);
    }

    private static String reasonFor(Task task, LocalDate today, double cap,
                                    AtcConfig config) {
        AtcTask atc = AtcTaskMapper.from(task, today, cap);
        double slack = AtcIndex.slack(atc, 0.0, config);
        double valueDensity = config.weightOf(task.getPriority()) / AtcIndex.processing(atc, config);
        String urgency = slack <= 0
                ? String.format(Locale.US, "maximum urgency (slack=%.1fh)", slack)
                : String.format(Locale.US, "%.1fh slack remaining", slack);
        return String.format(Locale.US, "value density w/p=%.2f; %s", valueDensity, urgency);
    }
}
