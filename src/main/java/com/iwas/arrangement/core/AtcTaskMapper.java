package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcTask;
import com.iwas.arrangement.time.CapacityHours;
import com.iwas.task.entity.Task;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reduces a domain {@link Task} to the three scheduling parameters the ATC
 * engine cares about. Outstanding work mirrors the workload v3 convention:
 * member-reported remaining hours when logged, otherwise the estimate.
 */
public final class AtcTaskMapper {

    private AtcTaskMapper() {
    }

    /**
     * @param today       reference instant (today) — the deadline runway is measured from here
     * @param dailyCap the lane's daily capacity in hours (8h × allocation%)
     */
    public static AtcTask from(Task task, LocalDate today, double dailyCap) {
        double p = processingHours(task);
        Double d = CapacityHours.dueHours(today, task.getDueDate(), dailyCap);
        return new AtcTask(task.getId(), task.getPriority(), p, d);
    }

    /** Outstanding effort in hours: reported remaining if logged, else the estimate; 0 if unknown. */
    private static double processingHours(Task task) {
        BigDecimal remaining = task.getReportedRemainingHours() != null
                ? task.getReportedRemainingHours() : task.getEstimatedHours();
        if (remaining == null || remaining.signum() <= 0) return 0.0;
        return remaining.doubleValue();
    }
}
