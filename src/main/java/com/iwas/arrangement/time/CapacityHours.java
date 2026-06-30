package com.iwas.arrangement.time;

import com.iwas.workload.service.ScheduleSimulator;

import java.time.LocalDate;
public final class CapacityHours {

    public static final double DEFAULT_DAILY_HOURS = 8.0;

    private CapacityHours() {
    }

    public static Double dueHours(LocalDate t0, LocalDate due, double dailyCap) {
        if (due == null) return null;
        double cap = dailyCap > 0 ? dailyCap : DEFAULT_DAILY_HOURS;
        long workdays = ScheduleSimulator.countWorkdays(t0, due);
        return workdays * cap;
    }
}
