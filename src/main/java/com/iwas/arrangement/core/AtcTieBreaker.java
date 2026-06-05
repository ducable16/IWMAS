package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcTask;

import java.util.Comparator;

/**
 * Deterministic tie-breaking when two tasks have indistinguishable ATC indices
 * (within {@link AtcIndex#EPS}). Order of precedence (§6 of the spec):
 *
 * <ol>
 *   <li>earlier {@code dueHours} first (EDD) — no deadline sorts last;</li>
 *   <li>higher priority first;</li>
 *   <li>smaller {@code id} first (absolute determinism).</li>
 * </ol>
 *
 * The resulting comparator orders tasks "best first", so the minimum element is
 * the one to schedule next.
 */
public final class AtcTieBreaker {

    /** Comparator ordering tasks best-first for equal indices. */
    public static final Comparator<AtcTask> BEST_FIRST = Comparator
            .comparingDouble(AtcTieBreaker::dueOrInfinity)
            .thenComparingInt(t -> priorityRank(t))
            .thenComparing(t -> idOrMax(t));

    private AtcTieBreaker() {
    }

    private static double dueOrInfinity(AtcTask t) {
        return t.dueHours() != null ? t.dueHours() : Double.POSITIVE_INFINITY;
    }

    private static int priorityRank(AtcTask t) {
        if (t.priority() == null) return 99;
        return switch (t.priority()) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private static long idOrMax(AtcTask t) {
        return t.id() != null ? t.id() : Long.MAX_VALUE;
    }

    /**
     * Returns the task that should be scheduled first among two equally-indexed
     * candidates.
     */
    public static AtcTask preferred(AtcTask a, AtcTask b) {
        return BEST_FIRST.compare(a, b) <= 0 ? a : b;
    }
}
