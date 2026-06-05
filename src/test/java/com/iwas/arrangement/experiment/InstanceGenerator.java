package com.iwas.arrangement.experiment;

import com.iwas.arrangement.model.AtcTask;
import com.iwas.task.enums.TaskPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates random weighted-tardiness instances in the style of the OR-Library
 * benchmark, controlled by two parameters:
 *
 * <ul>
 *   <li><b>tightness factor (TF)</b> — shifts due dates earlier, so more tasks
 *       are at risk of being late;</li>
 *   <li><b>relative range of due dates (RDD)</b> — spread of the deadlines.</li>
 * </ul>
 *
 * Processing times are U[1, pMax]; weights come from a random priority
 * (CRITICAL/HIGH/MEDIUM/LOW → 8/4/2/1), so weight dispersion is governed by the
 * priority distribution — matching the real system rather than abstract numbers.
 */
final class InstanceGenerator {

    private static final TaskPriority[] PRIORITIES = TaskPriority.values();

    private final Random random;

    InstanceGenerator(long seed) {
        this.random = new Random(seed);
    }

    /**
     * @param n   number of tasks
     * @param tf  tardiness factor in [0, 1] — higher = tighter deadlines
     * @param rdd relative range of due dates in (0, 1]
     */
    List<AtcTask> generate(int n, double tf, double rdd) {
        int[] p = new int[n];
        long totalP = 0;
        for (int i = 0; i < n; i++) {
            p[i] = 1 + random.nextInt(100);
            totalP += p[i];
        }

        double low = totalP * (1 - tf - rdd / 2.0);
        double high = totalP * (1 - tf + rdd / 2.0);

        List<AtcTask> tasks = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            TaskPriority priority = PRIORITIES[random.nextInt(PRIORITIES.length)];
            double due = low + random.nextDouble() * (high - low);
            tasks.add(new AtcTask((long) i, priority, p[i], Math.max(0.0, due)));
        }
        return tasks;
    }
}
