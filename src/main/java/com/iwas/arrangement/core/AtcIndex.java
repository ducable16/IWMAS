package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;

/**
 * The ATC priority index — shared by both the static {@link TardinessArranger}
 * and the online {@link AtcDispatcher}.
 *
 * <pre>
 *   Iⱼ(t) = (wⱼ / pⱼ) · exp( -max(0, slackⱼ) / (k · p̄) )
 *   slackⱼ = dⱼ - pⱼ - t
 * </pre>
 *
 * The first factor is WSPT value density (Smith's rule); the exponential is an
 * urgency multiplier in (0, 1] that saturates to 1 once a task can no longer be
 * comfortably finished by its deadline. This is a heuristic priority, not an
 * optimality guarantee.
 */
public final class AtcIndex {

    /** Indices within this distance are treated as a tie (see {@link AtcTieBreaker}). */
    public static final double EPS = 1e-9;

    private AtcIndex() {
    }

    /** Effective processing time used in the index — floored so {@code w/p} is finite. */
    public static double processing(AtcTask task, AtcConfig config) {
        return Math.max(task.processingHours(), config.minEstimateHours());
    }

    /** {@code slackⱼ = dⱼ - pⱼ - t}; +∞ for tasks with no deadline. */
    public static double slack(AtcTask task, double t, AtcConfig config) {
        double d = task.dueHours() != null ? task.dueHours() : Double.POSITIVE_INFINITY;
        return d - processing(task, config) - t;
    }

    /** Urgency factor {@code exp(-max(0, slack)/(k·p̄))} ∈ (0, 1]. */
    public static double urgency(double slack, double pBar, AtcConfig config) {
        if (slack <= 0.0) return 1.0;
        return Math.exp(-slack / (config.k() * pBar));
    }

    /**
     * Priority index {@code Iⱼ(t)} for one task at elapsed time {@code t}.
     *
     * @param pBar mean processing time over the candidate set (static; guarded ≥ 1)
     */
    public static double compute(AtcTask task, double t, double pBar, AtcConfig config) {
        double w = config.weightOf(task.priority());
        double p = processing(task, config);
        double urgency = urgency(slack(task, t, config), pBar, config);
        return (w / p) * urgency;
    }
}
