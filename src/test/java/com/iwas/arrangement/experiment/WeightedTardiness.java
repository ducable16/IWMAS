package com.iwas.arrangement.experiment;

import com.iwas.arrangement.core.AtcIndex;
import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;

import java.util.List;

/**
 * The objective being minimised: total weighted tardiness {@code ΣwⱼTⱼ} of a
 * given execution order on a single machine. Used by the validation experiment
 * to score any sequence (ATC, EDD, WSPT, brute-force optimum, random).
 */
final class WeightedTardiness {

    private WeightedTardiness() {
    }

    /** {@code ΣwⱼTⱼ} where {@code Tⱼ = max(0, Cⱼ - dⱼ)} for the supplied order. */
    static double objective(List<AtcTask> order, AtcConfig config) {
        double t = 0.0;
        double sum = 0.0;
        for (AtcTask task : order) {
            t += AtcIndex.processing(task, config);
            double d = task.dueHours() != null ? task.dueHours() : Double.POSITIVE_INFINITY;
            double tardiness = Math.max(0.0, t - d);
            sum += config.weightOf(task.priority()) * tardiness;
        }
        return sum;
    }
}
