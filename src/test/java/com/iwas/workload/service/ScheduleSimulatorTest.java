package com.iwas.workload.service;

import com.iwas.task.enums.TaskPriority;
import com.iwas.workload.service.ScheduleSimulator.LaneSimulation;
import com.iwas.workload.service.ScheduleSimulator.ScheduledTask;
import com.iwas.workload.service.ScheduleSimulator.TaskSchedule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleSimulatorTest {

    private final ScheduleSimulator sim = new ScheduleSimulator();

    /** A fixed Monday so all workday math is deterministic. */
    private static final LocalDate MON = LocalDate.of(2026, 6, 8).with(DayOfWeek.MONDAY);

    private static final BigDecimal CAP_8H = BigDecimal.valueOf(8);

    private static ScheduledTask task(long id, double remaining,
                                      LocalDate start, LocalDate due) {
        return new ScheduledTask(id, 100L, BigDecimal.valueOf(remaining),
                start, due, TaskPriority.MEDIUM);
    }

    private static TaskSchedule scheduleOf(LaneSimulation s, long taskId) {
        return s.schedules().stream()
                .filter(ts -> ts.taskId() == taskId)
                .findFirst().orElseThrow();
    }

    @Test
    void singleTaskFitsWithinCapacity() {
        // 16h at 8h/day → finishes on the 2nd workday, well before a Friday due.
        ScheduledTask t = task(1, 16, MON, ScheduleSimulator.addWorkdays(MON, 4));
        LaneSimulation result = sim.simulate(List.of(t), CAP_8H, MON);

        TaskSchedule ts = scheduleOf(result, 1);
        assertEquals(MON, ts.projectedStart());
        assertEquals(MON.plusDays(1), ts.projectedFinish());
        assertFalse(ts.willSlip());
        assertEquals(0, result.predictedLateTaskCount());
    }

    @Test
    void taskThatOverflowsItsDeadlineSlips() {
        // 40h at 8h/day = 5 workdays; due is only 2 workdays out.
        LocalDate due = ScheduleSimulator.addWorkdays(MON, 2);
        ScheduledTask t = task(1, 40, MON, due);
        LaneSimulation result = sim.simulate(List.of(t), CAP_8H, MON);

        TaskSchedule ts = scheduleOf(result, 1);
        assertTrue(ts.willSlip());
        assertEquals(ScheduleSimulator.addWorkdays(MON, 4), ts.projectedFinish());
        assertEquals(2, ts.lateByWorkdays());
        assertEquals(1, result.predictedLateTaskCount());
    }

    @Test
    void overdueTaskSlipsAndIsScheduledFromToday() {
        ScheduledTask t = task(1, 8, MON.minusDays(20), MON.minusDays(5));
        LaneSimulation result = sim.simulate(List.of(t), CAP_8H, MON);

        TaskSchedule ts = scheduleOf(result, 1);
        assertTrue(ts.willSlip());
        assertEquals(MON, ts.projectedStart());
        assertEquals(MON, ts.projectedFinish());
    }

    @Test
    void futureStartDateIsIgnored() {
        // startDate is not a release constraint — the task is worked from today regardless.
        LocalDate start = ScheduleSimulator.addWorkdays(MON, 5);
        ScheduledTask t = task(1, 8, start, ScheduleSimulator.addWorkdays(MON, 9));
        LaneSimulation result = sim.simulate(List.of(t), CAP_8H, MON);

        TaskSchedule ts = scheduleOf(result, 1);
        assertEquals(MON, ts.projectedStart(), "future startDate does not delay the work");
        assertEquals(MON, ts.projectedFinish());
        assertFalse(ts.willSlip());
    }

    @Test
    void futureStartTaskIsScheduledFromTodayNotItsStart() {
        // Under the old release-date model T2 would wait until its +5 start and slip.
        // Now both are available from today: T1 fills MON, T2 (40h) runs from the next
        // workday and finishes within its +7 due → no slip.
        ScheduledTask t1 = task(1, 8, MON, ScheduleSimulator.addWorkdays(MON, 1));
        ScheduledTask t2 = task(2, 40, ScheduleSimulator.addWorkdays(MON, 5),
                ScheduleSimulator.addWorkdays(MON, 7));
        LaneSimulation result = sim.simulate(List.of(t1, t2), CAP_8H, MON);

        assertFalse(scheduleOf(result, 1).willSlip());
        TaskSchedule s2 = scheduleOf(result, 2);
        assertEquals(MON.plusDays(1), s2.projectedStart(), "started the workday after T1, not at its +5 start");
        assertFalse(s2.willSlip(), "40h fits the window when worked from today");
    }

    @Test
    void customOrderCanIntroduceASlipThatEddAvoids() {
        // A is due today, B due in 5 workdays — each 8h, capacity 8h/day.
        ScheduledTask a = task(1, 8, MON, MON);
        ScheduledTask b = task(2, 8, MON, ScheduleSimulator.addWorkdays(MON, 5));

        LaneSimulation edd = sim.simulate(ScheduleSimulator.eddOrder(List.of(b, a)), CAP_8H, MON);
        assertEquals(0, edd.predictedLateTaskCount(), "EDD does A first → nothing slips");

        LaneSimulation custom = sim.simulate(List.of(b, a), CAP_8H, MON);
        assertEquals(1, custom.predictedLateTaskCount(), "B-before-A pushes A past its due");
        assertTrue(scheduleOf(custom, 1).willSlip());
    }

    @Test
    void zeroCapacityMakesEveryDueTaskSlip() {
        ScheduledTask t = task(1, 8, MON, ScheduleSimulator.addWorkdays(MON, 4));
        LaneSimulation result = sim.simulate(List.of(t), BigDecimal.ZERO, MON);

        TaskSchedule ts = scheduleOf(result, 1);
        assertTrue(ts.willSlip());
        assertNull(ts.projectedFinish());
    }

    @Test
    void taskWithoutDueDateNeverSlipsAndSortsLast() {
        ScheduledTask withDue = task(1, 8, MON, ScheduleSimulator.addWorkdays(MON, 3));
        ScheduledTask noDue = task(2, 8, MON, null);

        List<ScheduledTask> ordered = ScheduleSimulator.eddOrder(List.of(noDue, withDue));
        assertEquals(1, ordered.get(0).taskId(), "task with a due date comes first");
        assertEquals(2, ordered.get(1).taskId(), "no-due task sorts last");

        LaneSimulation result = sim.simulate(ordered, CAP_8H, MON);
        assertFalse(scheduleOf(result, 2).willSlip());
        assertNotNull(scheduleOf(result, 2).projectedFinish());
    }

    @Test
    void eddOrderBreaksTiesByPriority() {
        LocalDate due = ScheduleSimulator.addWorkdays(MON, 3);
        ScheduledTask low = new ScheduledTask(1L, 100L, BigDecimal.TEN, MON, due, TaskPriority.LOW);
        ScheduledTask critical = new ScheduledTask(2L, 100L, BigDecimal.TEN, MON, due, TaskPriority.CRITICAL);

        List<ScheduledTask> ordered = ScheduleSimulator.eddOrder(List.of(low, critical));
        assertEquals(2, ordered.get(0).taskId(), "same due date → CRITICAL before LOW");
    }
}
