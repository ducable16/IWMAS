package com.iwas.workload.service;

import com.iwas.task.enums.TaskPriority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScheduleSimulator {

    private static final double EPS = 1e-6;
    private static final int MAX_HORIZON_YEARS = 5;

    public record ScheduledTask(Long taskId, Long projectId, BigDecimal remaining,
                                LocalDate startDate, LocalDate dueDate, TaskPriority priority) {}

    public record TaskSchedule(Long taskId, LocalDate projectedStart, LocalDate projectedFinish,
                               boolean willSlip, long lateByWorkdays) {}

    public record LaneSimulation(List<TaskSchedule> schedules,
                                 int predictedLateTaskCount) {}

    public static boolean isWorkday(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    public static long countWorkdays(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) return 0;
        return from.datesUntil(to.plusDays(1)).filter(ScheduleSimulator::isWorkday).count();
    }

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

    public LaneSimulation simulate(List<ScheduledTask> ordered,
                                   BigDecimal subCapacityPerDay, LocalDate today) {
        List<ScheduledTask> workable = new ArrayList<>();
        for (ScheduledTask t : ordered) {
            if (t.remaining() != null && t.remaining().signum() > 0) workable.add(t);
        }

        double subCap = subCapacityPerDay == null ? 0.0 : subCapacityPerDay.doubleValue();

        List<TaskSchedule> schedules;
        if (subCap <= EPS) {
            schedules = new ArrayList<>();
            for (ScheduledTask t : workable) {
                schedules.add(new TaskSchedule(t.taskId(), null, null,
                        t.dueDate() != null, 0));
            }
        } else {
            schedules = runForward(workable, subCap, today);
        }

        int late = (int) schedules.stream().filter(TaskSchedule::willSlip).count();
        return new LaneSimulation(schedules, late);
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
                if (rem.get(t.taskId()) > EPS) { next = t; break; }
            }
            if (next == null) break;

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
                willSlip = true;
            } else {
                willSlip = finish.isAfter(t.dueDate());
                if (willSlip) lateBy = countWorkdays(t.dueDate().plusDays(1), finish);
            }
            result.add(new TaskSchedule(t.taskId(), projStart.get(t.taskId()), finish,
                    willSlip, lateBy));
        }
        return result;
    }

}
