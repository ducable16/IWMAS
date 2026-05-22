package com.iwas.workload.service;

import com.iwas.task.enums.TaskPriority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workload v3 engine — single-resource serial scheduling simulation.
 *
 * A member is modelled as one machine with a fixed daily sub-capacity per
 * project lane. Tasks are jobs with remaining effort, a release date
 * (startDate) and a deadline (dueDate). The simulation pours capacity, day by
 * day, into the highest-priority *released* task until it is done, then moves
 * to the next — producing a projected finish date for every task and a
 * per-task slip prediction.
 *
 * Ordering is a *forecasting hint*: {@link #simulate} treats the supplied
 * order as a priority list, not a rigid sequence. It is forward-only and never
 * assumes the order was historically followed — past reality is already baked
 * into each task's remaining hours. A member who works tasks out of order
 * simply produces different remaining values; the next simulation re-projects
 * from those.
 *
 * Pure and stateless: every input is a method argument, every output a record.
 */
@Component
public class ScheduleSimulator {

    /** Window (in workdays from today) used for the near-term tightness metric. */
    public static final int NEAR_TERM_WORKDAYS = 10;

    private static final double EPS = 1e-6;
    /** Hard stop for the forward loop — a task unfinished past this is unschedulable. */
    private static final int MAX_HORIZON_YEARS = 5;

    // ─── input / output records ───────────────────────────────────────────────

    /** A task as seen by the simulator. {@code startDate} null → released today. */
    public record ScheduledTask(Long taskId, Long projectId, BigDecimal remaining,
                                LocalDate startDate, LocalDate dueDate, TaskPriority priority) {}

    /** Per-task forecast. {@code projectedFinish} null → could not be scheduled. */
    public record TaskSchedule(Long taskId, LocalDate projectedStart, LocalDate projectedFinish,
                               boolean willSlip, long lateByWorkdays) {}

    /** Per-lane result: forecasts plus the order-independent tightness metrics. */
    public record LaneSimulation(List<TaskSchedule> schedules,
                                 BigDecimal nearTermPercent,
                                 BigDecimal overallPercent,
                                 int predictedLateTaskCount) {}

    // ─── workday calendar ─────────────────────────────────────────────────────

    public static boolean isWorkday(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    /** Inclusive count of Mon–Fri days in [from, to]. 0 when the range is empty. */
    public static long countWorkdays(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) return 0;
        return from.datesUntil(to.plusDays(1)).filter(ScheduleSimulator::isWorkday).count();
    }

    /** Adds {@code n} workdays to {@code start} (n=0 returns start unchanged). */
    public static LocalDate addWorkdays(LocalDate start, int n) {
        LocalDate d = start;
        int added = 0;
        while (added < n) {
            d = d.plusDays(1);
            if (isWorkday(d)) added++;
        }
        return d;
    }

    private static LocalDate nextWorkday(LocalDate d) {
        LocalDate n = d.plusDays(1);
        while (!isWorkday(n)) n = n.plusDays(1);
        return n;
    }

    private static LocalDate firstWorkdayOnOrAfter(LocalDate d) {
        LocalDate n = d;
        while (!isWorkday(n)) n = n.plusDays(1);
        return n;
    }

    // ─── ordering ──────────────────────────────────────────────────────────────

    /**
     * Earliest-Due-Date order — system-suggested optimal sequence. Jackson's
     * rule: on a single machine EDD minimises maximum lateness. Tasks with no
     * due date sort last; ties broken by priority (CRITICAL first) then id.
     */
    public static List<ScheduledTask> eddOrder(List<ScheduledTask> tasks) {
        List<ScheduledTask> copy = new ArrayList<>(tasks);
        copy.sort(Comparator
                .comparing((ScheduledTask t) -> t.dueDate() == null ? LocalDate.MAX : t.dueDate())
                .thenComparing(t -> priorityRank(t.priority()))
                .thenComparing(ScheduledTask::taskId));
        return copy;
    }

    private static int priorityRank(TaskPriority p) {
        if (p == null) return 99;
        return switch (p) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    // ─── simulation ────────────────────────────────────────────────────────────

    /**
     * Runs the forward simulation for one lane.
     *
     * @param ordered  tasks in the desired priority order (EDD or member custom)
     * @param subCapacityPerDay daily capacity of this lane (8h × allocation%)
     * @param today    forecast start; treated as a full workday
     */
    public LaneSimulation simulate(List<ScheduledTask> ordered,
                                   BigDecimal subCapacityPerDay, LocalDate today) {
        List<ScheduledTask> workable = new ArrayList<>();
        for (ScheduledTask t : ordered) {
            if (t.remaining() != null && t.remaining().signum() > 0) workable.add(t);
        }

        double subCap = subCapacityPerDay == null ? 0.0 : subCapacityPerDay.doubleValue();
        double[] pct = tightness(workable, subCap, today);

        List<TaskSchedule> schedules;
        if (subCap <= EPS) {
            // No capacity in this lane — nothing can be scheduled.
            schedules = new ArrayList<>();
            for (ScheduledTask t : workable) {
                schedules.add(new TaskSchedule(t.taskId(), null, null,
                        t.dueDate() != null, 0));
            }
        } else {
            schedules = runForward(workable, subCap, today);
        }

        int late = (int) schedules.stream().filter(TaskSchedule::willSlip).count();
        return new LaneSimulation(schedules,
                BigDecimal.valueOf(pct[0]).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(pct[1]).setScale(2, RoundingMode.HALF_UP),
                late);
    }

    private List<TaskSchedule> runForward(List<ScheduledTask> ordered,
                                          double subCap, LocalDate today) {
        Map<Long, Double> rem = new HashMap<>();
        Map<Long, LocalDate> projStart = new HashMap<>();
        Map<Long, LocalDate> projFinish = new HashMap<>();
        for (ScheduledTask t : ordered) rem.put(t.taskId(), t.remaining().doubleValue());

        LocalDate cursor = firstWorkdayOnOrAfter(today);
        double dayLeft = subCap;
        LocalDate hardStop = today.plusYears(MAX_HORIZON_YEARS);

        while (rem.values().stream().anyMatch(h -> h > EPS) && !cursor.isAfter(hardStop)) {
            ScheduledTask next = null;
            for (ScheduledTask t : ordered) {
                if (rem.get(t.taskId()) <= EPS) continue;
                if (t.startDate() == null || !t.startDate().isAfter(cursor)) { next = t; break; }
            }

            if (next == null) {
                // Everything left is released only in the future — jump the cursor.
                LocalDate earliest = null;
                for (ScheduledTask t : ordered) {
                    if (rem.get(t.taskId()) <= EPS || t.startDate() == null) continue;
                    if (earliest == null || t.startDate().isBefore(earliest)) earliest = t.startDate();
                }
                if (earliest == null) break;
                cursor = firstWorkdayOnOrAfter(earliest);
                dayLeft = subCap;
                continue;
            }

            projStart.putIfAbsent(next.taskId(), cursor);
            double work = Math.min(dayLeft, rem.get(next.taskId()));
            rem.put(next.taskId(), rem.get(next.taskId()) - work);
            dayLeft -= work;

            if (rem.get(next.taskId()) <= EPS) projFinish.put(next.taskId(), cursor);
            if (dayLeft <= EPS) {
                cursor = nextWorkday(cursor);
                dayLeft = subCap;
            }
        }

        List<TaskSchedule> result = new ArrayList<>();
        for (ScheduledTask t : ordered) {
            LocalDate finish = projFinish.get(t.taskId());
            boolean willSlip;
            long lateBy = 0;
            if (t.dueDate() == null) {
                willSlip = false;
            } else if (finish == null) {
                willSlip = true; // unfinished within the horizon
            } else {
                willSlip = finish.isAfter(t.dueDate());
                if (willSlip) lateBy = countWorkdays(t.dueDate().plusDays(1), finish);
            }
            result.add(new TaskSchedule(t.taskId(), projStart.get(t.taskId()), finish,
                    willSlip, lateBy));
        }
        return result;
    }

    /**
     * Order-independent tightness metric. For each task with a deadline,
     * cumulative remaining work (sorted by deadline, overdue clamped to today)
     * over the capacity available until that deadline. Returns {nearTerm, overall}
     * as the worst (max) ratio in percent.
     */
    private double[] tightness(List<ScheduledTask> workable, double subCap, LocalDate today) {
        if (subCap <= EPS) return new double[]{0.0, 0.0};

        List<ScheduledTask> withDue = new ArrayList<>();
        for (ScheduledTask t : workable) {
            if (t.dueDate() != null) withDue.add(t);
        }
        withDue.sort(Comparator.comparing(t -> effectiveDue(t.dueDate(), today)));

        LocalDate nearHorizon = addWorkdays(today, NEAR_TERM_WORKDAYS - 1);
        double cumulative = 0.0;
        double nearMax = 0.0;
        double overallMax = 0.0;
        for (ScheduledTask t : withDue) {
            cumulative += t.remaining().doubleValue();
            LocalDate due = effectiveDue(t.dueDate(), today);
            long capDays = Math.max(1, countWorkdays(today, due));
            double ratio = cumulative / (capDays * subCap) * 100.0;
            overallMax = Math.max(overallMax, ratio);
            if (!due.isAfter(nearHorizon)) nearMax = Math.max(nearMax, ratio);
        }
        return new double[]{nearMax, overallMax};
    }

    private static LocalDate effectiveDue(LocalDate due, LocalDate today) {
        return due.isBefore(today) ? today : due;
    }
}
