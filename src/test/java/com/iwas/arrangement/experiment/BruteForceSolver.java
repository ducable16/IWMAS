package com.iwas.arrangement.experiment;

import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Exhaustive {@code n!} solver for {@code 1 ‖ ΣwⱼTⱼ}. Tries every permutation
 * and returns the true minimum objective — the optimum used as the yardstick
 * for measuring the ATC heuristic's optimality gap. Only tractable for small
 * {@code n} (≤ 10); this is a validation tool, never a production path.
 */
final class BruteForceSolver {

    private BruteForceSolver() {
    }

    /** Minimum {@code ΣwⱼTⱼ} over all orderings of the tasks. */
    static double optimum(List<AtcTask> tasks, AtcConfig config) {
        double[] best = {Double.POSITIVE_INFINITY};
        permute(new ArrayList<>(tasks), 0, new ArrayList<>(), config, best);
        return best[0];
    }

    private static void permute(List<AtcTask> remaining, int depth, List<AtcTask> current,
                                AtcConfig config, double[] best) {
        if (remaining.isEmpty()) {
            best[0] = Math.min(best[0], WeightedTardiness.objective(current, config));
            return;
        }
        for (int i = 0; i < remaining.size(); i++) {
            AtcTask picked = remaining.remove(i);
            current.add(picked);
            permute(remaining, depth + 1, current, config, best);
            current.remove(current.size() - 1);
            remaining.add(i, picked);
        }
    }
}
