package com.iwas.arrangement.core;

import com.iwas.arrangement.model.ArrangedTask;
import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import com.iwas.task.enums.TaskPriority;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TardinessArrangerTest {

    private final TardinessArranger arranger = new TardinessArranger();

    private static AtcConfig config(double criticalWeight) {
        Map<TaskPriority, Double> weights = new EnumMap<>(TaskPriority.class);
        weights.put(TaskPriority.CRITICAL, criticalWeight);
        weights.put(TaskPriority.HIGH, 4.0);
        weights.put(TaskPriority.MEDIUM, 2.0);
        weights.put(TaskPriority.LOW, 1.0);
        return new AtcConfig(weights, 2.0);
    }

    /** A: w8 p4 d6, B: w4 p2 d3, C: w1 p1 d10 → expected B, A, C. */
    private static List<AtcTask> classicInstance() {
        return new ArrayList<>(List.of(
                new AtcTask(1L, TaskPriority.CRITICAL, 4, 6.0),
                new AtcTask(2L, TaskPriority.HIGH, 2, 3.0),
                new AtcTask(3L, TaskPriority.LOW, 1, 10.0)));
    }

    @Test
    void ordersClassicInstanceBeforeAC() {
        List<Long> order = arranger.orderTaskIds(classicInstance(), config(8.0));
        assertEquals(List.of(2L, 1L, 3L), order, "B should precede A despite A being CRITICAL");
    }

    @Test
    void wideWeightRatioBehavesLexicographically() {
        // CRITICAL = 100 → A (the only CRITICAL) must come first.
        List<Long> order = arranger.orderTaskIds(classicInstance(), config(100.0));
        assertEquals(1L, order.get(0), "with CRITICAL=100, A must lead");
    }

    @Test
    void emptyQueueReturnsEmpty() {
        assertTrue(arranger.arrange(List.of(), config(8.0)).isEmpty());
    }

    @Test
    void outputIsAPermutationOfInput() {
        List<AtcTask> tasks = classicInstance();
        Set<Long> input = new HashSet<>();
        for (AtcTask t : tasks) input.add(t.id());

        List<Long> order = arranger.orderTaskIds(tasks, config(8.0));
        assertEquals(input.size(), order.size(), "no tasks lost or duplicated");
        assertEquals(input, new HashSet<>(order));
    }

    @Test
    void isDeterministic() {
        AtcConfig config = config(8.0);
        assertEquals(arranger.orderTaskIds(classicInstance(), config),
                arranger.orderTaskIds(classicInstance(), config));
    }

    @Test
    void allOverdueFallsBackToWsptOrder() {
        // Every task already past due (dueHours ≤ 0) → urgency=1 → pure WSPT (w/p desc).
        List<AtcTask> tasks = new ArrayList<>(List.of(
                new AtcTask(1L, TaskPriority.LOW, 1, -5.0),     // w/p = 1
                new AtcTask(2L, TaskPriority.CRITICAL, 4, -5.0), // w/p = 2
                new AtcTask(3L, TaskPriority.HIGH, 1, -5.0)));   // w/p = 4
        assertEquals(List.of(3L, 2L, 1L), arranger.orderTaskIds(tasks, config(8.0)));
    }

    @Test
    void deadlinelessTasksSinkToTheEnd() {
        List<AtcTask> tasks = new ArrayList<>(List.of(
                new AtcTask(1L, TaskPriority.HIGH, 2, null),
                new AtcTask(2L, TaskPriority.HIGH, 2, 3.0)));
        assertEquals(List.of(2L, 1L), arranger.orderTaskIds(tasks, config(8.0)));
    }

    @Test
    void missingEstimateIsFlaggedAndScheduled() {
        List<AtcTask> tasks = new ArrayList<>(List.of(
                new AtcTask(1L, TaskPriority.HIGH, 0, 5.0)));
        List<ArrangedTask> result = arranger.arrange(tasks, config(8.0));
        assertEquals(1, result.size());
        assertTrue(result.get(0).estimateDefaulted(), "zero estimate must raise the warning flag");
    }

    @Test
    void projectionsAreCumulativeAndNonPreemptive() {
        List<ArrangedTask> result = arranger.arrange(classicInstance(), config(8.0));
        // Order B(p2), A(p4), C(p1): finishes at 2, 6, 7.
        double previousFinish = 0;
        for (ArrangedTask t : result) {
            assertEquals(previousFinish, t.projectedStartHours(), 1e-9);
            assertTrue(t.projectedFinishHours() > t.projectedStartHours());
            previousFinish = t.projectedFinishHours();
        }
        assertEquals(7.0, previousFinish, 1e-9);
    }
}
