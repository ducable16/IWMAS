package com.iwas.arrangement.service;

import com.iwas.arrangement.config.AtcProperties;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
    private final AtcProperties properties;

    /** Static arrangement — suggested execution order for the lane. */
    @Transactional(readOnly = true)
    public ArrangeResponse arrangeLane(Long projectId, Long assigneeId,
                                       Double kOverride, Map<TaskPriority, Double> weightOverrides) {
        Lane lane = loadLane(projectId, assigneeId);
        AtcConfig config = AtcConfig.from(properties).withOverrides(kOverride, weightOverrides);
        LocalDate today = LocalDate.now();
        double orderingCap = lane.dailyCap() > 0 ? lane.dailyCap() : DAILY_HOURS;

        List<AtcTask> atcTasks = lane.workable().stream()
                .map(t -> AtcTaskMapper.from(t, today, orderingCap))
                .toList();
        List<ArrangedTask> arranged = arranger.arrange(atcTasks, config);

        Map<Long, TaskSchedule> calendar = projectCalendar(lane, arranged, today);
        Map<Long, Task> byId = byId(lane.workable());

        List<ArrangeResponse.Item> items = new ArrayList<>();
        for (ArrangedTask a : arranged) {
            Task task = byId.get(a.taskId());
            TaskSchedule sched = calendar.get(a.taskId());
            items.add(new ArrangeResponse.Item(
                    a.taskId(),
                    task.getTitle(),
                    a.position(),
                    task.getPriority(),
                    a.priorityIndex(),
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

        AtcConfig config = AtcConfig.from(properties).withOverrides(kOverride, weightOverrides);
        LocalDate today = LocalDate.now();
        double orderingCap = lane.dailyCap() > 0 ? lane.dailyCap() : DAILY_HOURS;

        List<AtcTask> eligible = lane.workable().stream()
                .map(t -> AtcTaskMapper.from(t, today, orderingCap))
                .toList();
        Optional<AtcTask> next = dispatcher.nextTask(eligible, config);
        if (next.isEmpty()) return NextTaskResponse.empty(projectId, assigneeId);

        Task task = byId(lane.workable()).get(next.get().id());
        double pBar = TardinessArranger.meanProcessing(eligible, config);
        double index = AtcIndex.compute(next.get(), 0.0, pBar, config);
        return new NextTaskResponse(projectId, assigneeId, false,
                task.getId(), task.getTitle(), task.getPriority(), index,
                reasonFor(task, today, orderingCap, config, pBar));
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

    /** Runs the workload simulator over the ATC order to attach calendar dates. */
    private Map<Long, TaskSchedule> projectCalendar(Lane lane, List<ArrangedTask> arranged,
                                                    LocalDate today) {
        if (lane.dailyCap() <= 0 || arranged.isEmpty()) return Map.of();
        Map<Long, Task> byId = byId(lane.workable());
        List<ScheduledTask> ordered = arranged.stream()
                .map(a -> toScheduledTask(byId.get(a.taskId())))
                .toList();
        LaneSimulation sim = scheduleSimulator.simulate(ordered,
                capacityBd(lane.dailyCap()), today);
        Map<Long, TaskSchedule> result = new HashMap<>();
        for (TaskSchedule ts : sim.schedules()) result.put(ts.taskId(), ts);
        return result;
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static Map<Long, Task> byId(List<Task> tasks) {
        Map<Long, Task> map = new HashMap<>();
        for (Task t : tasks) map.put(t.getId(), t);
        return map;
    }

    private static ScheduledTask toScheduledTask(Task t) {
        return new ScheduledTask(t.getId(), t.getProjectId(), resolveRemaining(t),
                t.getStartDate(), t.getDueDate(), t.getPriority());
    }

    private static BigDecimal resolveRemaining(Task t) {
        return t.getReportedRemainingHours() != null
                ? t.getReportedRemainingHours() : t.getEstimatedHours();
    }

    private static boolean isWorkable(Task t) {
        BigDecimal r = resolveRemaining(t);
        return r != null && r.signum() > 0;
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
                                    AtcConfig config, double pBar) {
        AtcTask atc = AtcTaskMapper.from(task, today, cap);
        double slack = AtcIndex.slack(atc, 0.0, config);
        double valueDensity = config.weightOf(task.getPriority()) / AtcIndex.processing(atc, config);
        String urgency = slack <= 0
                ? String.format(Locale.US, "maximum urgency (slack=%.1fh)", slack)
                : String.format(Locale.US, "%.1fh slack remaining", slack);
        return String.format(Locale.US, "value density w/p=%.2f; %s", valueDensity, urgency);
    }
}
