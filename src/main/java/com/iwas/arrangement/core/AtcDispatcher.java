package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Online ("dispatching") counterpart of {@link TardinessArranger}. Instead of
 * building the whole sequence up front it answers a single question — "which
 * task should this member pick up next?" — by evaluating the same
 * {@link AtcIndex} over the currently eligible tasks at the current time.
 *
 * <p>Recompute on each event (a task finishes, a new task is assigned, a
 * priority changes) and re-call. Dependency / blocked filtering, when it
 * exists, happens before tasks reach this method (the {@code eligible} set).
 */
@Component
public class AtcDispatcher {

    /** The single highest-index eligible task at elapsed time {@code t}. */
    public Optional<AtcTask> nextTask(List<AtcTask> eligible, double t, AtcConfig config) {
        if (eligible == null || eligible.isEmpty()) return Optional.empty();

        double pAverage = TardinessArranger.meanProcessing(eligible);
        AtcTask best = null;
        double bestIndex = Double.NEGATIVE_INFINITY;
        for (AtcTask candidate : eligible) {
            double index = AtcIndex.compute(candidate, t, pAverage, config);
            if (best == null || index > bestIndex + AtcIndex.EPS) {
                best = candidate;
                bestIndex = index;
            } else if (index > bestIndex - AtcIndex.EPS) {
                best = AtcTieBreaker.preferred(best, candidate);
                bestIndex = AtcIndex.compute(best, t, pAverage, config);
            }
        }
        return Optional.ofNullable(best);
    }

    /** Convenience: dispatch at the current moment ({@code t = 0} relative to t0). */
    public Optional<AtcTask> nextTask(List<AtcTask> eligible, AtcConfig config) {
        return nextTask(eligible, 0.0, config);
    }
}
