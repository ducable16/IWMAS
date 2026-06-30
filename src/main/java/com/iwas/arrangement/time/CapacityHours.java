package com.iwas.arrangement.time;

import com.iwas.workload.service.ScheduleSimulator;

import java.time.LocalDate;
public final class CapacityHours {

    private CapacityHours() {
    }

    public static Double dueHours(LocalDate t0, LocalDate due, double dailyCap) {
        if (due == null) return null;
        long workdays = ScheduleSimulator.countWorkdays(t0, due);
        return workdays * dailyCap;
    }
}
