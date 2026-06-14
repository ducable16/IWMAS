package com.iwas.arrangement.core;

import com.iwas.arrangement.model.ArrangedTask;
import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Static ATC construction heuristic for {@code 1 ‖ ΣwⱼTⱼ}. Builds a full
 * execution order in one pass: at each step it picks the unscheduled task with
 * the highest {@link AtcIndex}, advances the clock by that task's processing
 * time (non-preemptive, non-idling) and repeats.
 *
 * <p>This produces a <em>high-quality heuristic</em> order, not a proven
 * optimum — the underlying problem is strongly NP-hard. The mean processing
 * time {@code p̄} that normalises the slack scale is recomputed each step over
 * the tasks still waiting, matching the original rule's "average processing
 * time of the waiting jobs" (Rachamadugu &amp; Morton 1981).
 *
 * <p>Pure and deterministic: same input + config always yields the same order.
 */
@Component
public class TardinessArranger {

    /** Full arrangement with explainability metadata for every task. */
    public List<ArrangedTask> arrange(List<AtcTask> tasks, AtcConfig config) {
        List<ArrangedTask> result = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) return result;

        List<AtcTask> remaining = new ArrayList<>(tasks);
        double t = 0.0;
        int position = 0;

        while (!remaining.isEmpty()) {
            double pAverage = meanProcessing(remaining, config);
            AtcTask best = null;
            double bestIndex = Double.NEGATIVE_INFINITY;
            for (AtcTask candidate : remaining) {
                double index = AtcIndex.compute(candidate, t, pAverage, config);
                if (best == null || index > bestIndex + AtcIndex.EPS) {
                    best = candidate;
                    bestIndex = index;
                } else if (index > bestIndex - AtcIndex.EPS) {
                    best = AtcTieBreaker.preferred(best, candidate);
                    bestIndex = AtcIndex.compute(best, t, pAverage, config);
                }
            }

            double p = AtcIndex.processing(best, config);
            double slack = AtcIndex.slack(best, t, config);
            double start = t;
            t += p;
            double finish = t;
            double tardiness = best.dueHours() != null
                    ? Math.max(0.0, finish - best.dueHours()) : 0.0;

            result.add(new ArrangedTask(best.id(), position++, slack,
                    start, finish, tardiness, best.processingHours() <= 0.0,
                    buildReason(best, config, slack, pAverage)));
            remaining.remove(best);
        }
        return result;
    }

    /** Just the ordered task ids — used when only the sequence is needed. */
    public List<Long> orderTaskIds(List<AtcTask> tasks, AtcConfig config) {
        return arrange(tasks, config).stream().map(ArrangedTask::taskId).toList();
    }

    /** Mean effective processing time; guarded to ≥ 1 so the slack scale is finite. */
    public static double meanProcessing(List<AtcTask> tasks, AtcConfig config) {
        double sum = 0.0;
        for (AtcTask t : tasks) sum += AtcIndex.processing(t, config);
        double mean = sum / tasks.size();
        return mean <= 0.0 ? 1.0 : mean;
    }

    private static String buildReason(AtcTask task, AtcConfig config, double slack, double pAverage) {
        double valueDensity = config.weightOf(task.priority()) / AtcIndex.processing(task, config);
        String urgencyNote;
        if (task.dueHours() == null) {
            urgencyNote = "no deadline (sinks to the end of its tier)";
        } else if (slack <= 0) {
            urgencyNote = String.format(Locale.US, "maximum urgency (slack=%.1fh ≤ 0)", slack);
        } else {
            double urgency = AtcIndex.urgency(slack, pAverage, config);
            urgencyNote = String.format(Locale.US, "%.1fh slack remaining (urgency factor=%.2f)", slack, urgency);
        }
        return String.format(Locale.US, "value density w/p=%.2f; %s", valueDensity, urgencyNote);
    }
}
