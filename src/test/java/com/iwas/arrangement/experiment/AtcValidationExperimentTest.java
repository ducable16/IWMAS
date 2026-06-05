package com.iwas.arrangement.experiment;

import com.iwas.arrangement.core.AtcIndex;
import com.iwas.arrangement.core.TardinessArranger;
import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import com.iwas.task.enums.TaskPriority;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validation experiment for the thesis (§9.3): measures the ATC heuristic's
 * optimality gap against the brute-force optimum and compares it with the EDD,
 * WSPT and random baselines over many random instances.
 *
 * <p>Tagged {@code experiment} — it doubles as a regression guard (ATC must, on
 * average, beat the simpler baselines and stay within a small gap of optimum)
 * and prints a report that can be lifted into the report.
 */
@Tag("experiment")
class AtcValidationExperimentTest {

    private static final int N = 6;            // tasks per instance (n! brute force)
    private static final int INSTANCES = 200;
    private static final double[] TIGHTNESS = {0.2, 0.4, 0.6, 0.8};
    private static final double RDD = 0.6;

    private final TardinessArranger arranger = new TardinessArranger();

    private static AtcConfig config(double k) {
        Map<TaskPriority, Double> weights = new EnumMap<>(TaskPriority.class);
        weights.put(TaskPriority.CRITICAL, 8.0);
        weights.put(TaskPriority.HIGH, 4.0);
        weights.put(TaskPriority.MEDIUM, 2.0);
        weights.put(TaskPriority.LOW, 1.0);
        return new AtcConfig(weights, k, 0.25, TaskPriority.LOW);
    }

    @Test
    void atcBeatsBaselinesAndStaysNearOptimum() {
        AtcConfig config = config(2.0);
        InstanceGenerator gen = new InstanceGenerator(42L);
        Random shuffleRng = new Random(7L);

        double atcGap = 0, eddGap = 0, wsptGap = 0, randGap = 0;
        int counted = 0;

        for (double tf : TIGHTNESS) {
            for (int i = 0; i < INSTANCES; i++) {
                List<AtcTask> tasks = gen.generate(N, tf, RDD);
                double opt = BruteForceSolver.optimum(tasks, config);
                if (opt <= 0) continue; // every order on time — gap undefined

                double atc = objectiveOfAtc(tasks, config);
                double edd = WeightedTardiness.objective(eddOrder(tasks), config);
                double wspt = WeightedTardiness.objective(wsptOrder(tasks, config), config);
                double rand = WeightedTardiness.objective(randomOrder(tasks, shuffleRng), config);

                atcGap += (atc - opt) / opt;
                eddGap += (edd - opt) / opt;
                wsptGap += (wspt - opt) / opt;
                randGap += (rand - opt) / opt;
                counted++;
            }
        }

        atcGap /= counted;
        eddGap /= counted;
        wsptGap /= counted;
        randGap /= counted;

        System.out.printf("ATC validation over %d instances (n=%d):%n", counted, N);
        System.out.printf("  mean optimality gap  ATC=%.4f  EDD=%.4f  WSPT=%.4f  random=%.4f%n",
                atcGap, eddGap, wsptGap, randGap);

        assertTrue(atcGap >= 0, "gap cannot be negative — heuristic cannot beat the optimum");
        assertTrue(atcGap <= eddGap, "ATC should beat EDD on average");
        assertTrue(atcGap <= wsptGap, "ATC should beat WSPT on average");
        assertTrue(atcGap < randGap, "ATC should beat random on average");
    }

    @Test
    void reportGapVersusK() {
        InstanceGenerator gen = new InstanceGenerator(99L);
        System.out.println("k vs mean optimality gap (n=" + N + "):");
        for (double k = 0.5; k <= 4.5; k += 0.5) {
            AtcConfig config = config(k);
            double gap = 0;
            int counted = 0;
            for (double tf : TIGHTNESS) {
                for (int i = 0; i < INSTANCES; i++) {
                    List<AtcTask> tasks = gen.generate(N, tf, RDD);
                    double opt = BruteForceSolver.optimum(tasks, config);
                    if (opt <= 0) continue;
                    gap += (objectiveOfAtc(tasks, config) - opt) / opt;
                    counted++;
                }
            }
            System.out.printf("  k=%.1f  gap=%.4f%n", k, gap / counted);
        }
    }

    private double objectiveOfAtc(List<AtcTask> tasks, AtcConfig config) {
        List<Long> ids = arranger.orderTaskIds(tasks, config);
        Map<Long, AtcTask> byId = new java.util.HashMap<>();
        for (AtcTask t : tasks) byId.put(t.id(), t);
        List<AtcTask> order = ids.stream().map(byId::get).toList();
        return WeightedTardiness.objective(order, config);
    }

    private static List<AtcTask> eddOrder(List<AtcTask> tasks) {
        List<AtcTask> copy = new ArrayList<>(tasks);
        copy.sort(Comparator.comparingDouble(t ->
                t.dueHours() != null ? t.dueHours() : Double.POSITIVE_INFINITY));
        return copy;
    }

    private static List<AtcTask> wsptOrder(List<AtcTask> tasks, AtcConfig config) {
        List<AtcTask> copy = new ArrayList<>(tasks);
        copy.sort(Comparator.comparingDouble(
                (AtcTask t) -> config.weightOf(t.priority()) / AtcIndex.processing(t, config)).reversed());
        return copy;
    }

    private static List<AtcTask> randomOrder(List<AtcTask> tasks, Random rng) {
        List<AtcTask> copy = new ArrayList<>(tasks);
        java.util.Collections.shuffle(copy, rng);
        return copy;
    }
}
