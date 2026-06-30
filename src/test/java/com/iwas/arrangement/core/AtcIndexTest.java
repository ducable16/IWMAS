package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import com.iwas.task.enums.TaskPriority;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtcIndexTest {

    private static AtcConfig config() {
        Map<TaskPriority, Double> weights = new EnumMap<>(TaskPriority.class);
        weights.put(TaskPriority.CRITICAL, 8.0);
        weights.put(TaskPriority.HIGH, 4.0);
        weights.put(TaskPriority.MEDIUM, 2.0);
        weights.put(TaskPriority.LOW, 1.0);
        return new AtcConfig(weights, 2.0);
    }

    @Test
    void matchesWorkedExampleFromSpec() {
        AtcConfig config = config();
        double pAverage = (4 + 2 + 1) / 3.0; // 2.333

        AtcTask a = new AtcTask(1L, TaskPriority.CRITICAL, 4, 6.0);
        AtcTask b = new AtcTask(2L, TaskPriority.HIGH, 2, 3.0);
        AtcTask c = new AtcTask(3L, TaskPriority.LOW, 1, 10.0);

        assertEquals(1.303, AtcIndex.compute(a, 0, pAverage, config), 0.01);
        assertEquals(1.614, AtcIndex.compute(b, 0, pAverage, config), 0.01);
        assertEquals(0.145, AtcIndex.compute(c, 0, pAverage, config), 0.01);
    }

    @Test
    void urgencyIsOneWhenSlackNonPositive() {
        assertEquals(1.0, AtcIndex.urgency(0.0, 3.0, config()));
        assertEquals(1.0, AtcIndex.urgency(-5.0, 3.0, config()));
    }

    @Test
    void urgencyStaysInOpenZeroToOneInterval() {
        AtcConfig config = config();
        for (double slack = 0.1; slack < 100; slack += 0.7) {
            double u = AtcIndex.urgency(slack, 3.0, config);
            assertTrue(u > 0.0 && u <= 1.0, "urgency must be in (0,1] but was " + u);
        }
    }

    @Test
    void noDeadlineDrivesUrgencyToZero() {
        AtcTask noDue = new AtcTask(1L, TaskPriority.HIGH, 2, null);
        assertEquals(0.0, AtcIndex.compute(noDue, 0, 3.0, config()), 1e-9);
    }
}
