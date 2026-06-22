package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcTask;
import com.iwas.arrangement.time.CapacityHours;
import com.iwas.task.entity.Task;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reduces a domain {@link Task} to the three scheduling parameters the ATC
 * engine cares about.
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

    private static double processingHours(Task task) {
        BigDecimal est = task.getEstimatedHours();
        if (est == null || est.signum() <= 0) return 0.0;
        return est.doubleValue();
    }
}
