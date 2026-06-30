package com.iwas.arrangement.core;

import com.iwas.arrangement.model.ArrangedTask;
import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
@Component
public class TardinessArranger {

    public List<ArrangedTask> arrange(List<AtcTask> tasks, AtcConfig config) {
        List<ArrangedTask> result = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) return result;

        List<AtcTask> remaining = new ArrayList<>(tasks);
        double t = 0.0;
        int position = 0;

        while (!remaining.isEmpty()) {
            double pAverage = meanProcessing(remaining);
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

            double p = best.processingHours();
            double slack = AtcIndex.slack(best, t);
            double start = t;
            t += p;
            double finish = t;
            double tardiness = best.dueHours() != null
                    ? Math.max(0.0, finish - best.dueHours()) : 0.0;

            result.add(new ArrangedTask(best.id(), position++, slack,
                    start, finish, tardiness, best.processingHours() <= 0.0));
            remaining.remove(best);
        }
        return result;
    }

    public List<Long> orderTaskIds(List<AtcTask> tasks, AtcConfig config) {
        return arrange(tasks, config).stream().map(ArrangedTask::taskId).toList();
    }

    public static double meanProcessing(List<AtcTask> tasks) {
        double sum = 0.0;
        for (AtcTask t : tasks) sum += t.processingHours();
        double mean = sum / tasks.size();
        return mean <= 0.0 ? 1.0 : mean;
    }
}
