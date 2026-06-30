package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AtcDispatcher {

    public Optional<AtcTask> nextTask(List<AtcTask> atcTasks, double t, AtcConfig config) {
        if (atcTasks == null || atcTasks.isEmpty()) return Optional.empty();

        double pAverage = TardinessArranger.meanProcessing(atcTasks);
        AtcTask best = null;
        double bestIndex = Double.NEGATIVE_INFINITY;
        for (AtcTask candidate : atcTasks) {
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

    public Optional<AtcTask> nextTask(List<AtcTask> atcTasks, AtcConfig config) {
        return nextTask(atcTasks, 0.0, config);
    }
}
