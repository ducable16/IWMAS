package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AtcDispatcher {

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

    public Optional<AtcTask> nextTask(List<AtcTask> eligible, AtcConfig config) {
        return nextTask(eligible, 0.0, config);
    }
}
