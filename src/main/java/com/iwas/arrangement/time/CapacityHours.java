package com.iwas.arrangement.time;

import com.iwas.workload.service.ScheduleSimulator;

import java.time.LocalDate;

/**
 * Converts calendar deadlines into the engine's time unit: <em>available
 * work-hours</em>. A member's runway until a deadline is the number of
 * remaining workdays times their daily lane capacity ({@code 8h × allocation%}),
 * which is the same currency as a task's processing time (also work-hours).
 *
 * <p>The workday calendar (Mon–Fri, no holidays) is reused from
 * {@link ScheduleSimulator} so arrangement and forecasting stay consistent.
 */
public final class CapacityHours {

    /** Fallback capacity used only for ordering when a lane has no allocation. */
    public static final double DEFAULT_DAILY_HOURS = 8.0;

    private CapacityHours() {
    }

    /**
     * Work-hours of runway between {@code t0} (today, treated as a full workday)
     * and {@code due}.
     *
     * @return {@code null} when {@code due} is null (no deadline → +∞); 0 when the
     *         deadline has already passed (fully overdue → maximum urgency)
     */
    public static Double dueHours(LocalDate t0, LocalDate due, double dailyCap) {
        if (due == null) return null;
        double cap = dailyCap > 0 ? dailyCap : DEFAULT_DAILY_HOURS;
        long workdays = ScheduleSimulator.countWorkdays(t0, due);
        return workdays * cap;
    }
}
