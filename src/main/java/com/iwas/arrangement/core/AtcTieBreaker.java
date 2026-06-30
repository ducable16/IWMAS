package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcTask;

import java.util.Comparator;

public final class AtcTieBreaker {

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

    public static AtcTask preferred(AtcTask a, AtcTask b) {
        return BEST_FIRST.compare(a, b) <= 0 ? a : b;
    }
}
