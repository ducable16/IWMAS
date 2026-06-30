package com.iwas.arrangement.core;

import com.iwas.arrangement.model.AtcConfig;
import com.iwas.arrangement.model.AtcTask;

public final class AtcIndex {

    public static final double EPS = 1e-9;

    private AtcIndex() {
    }

    public static double processing(AtcTask task, AtcConfig config) {
        return task.processingHours();
    }

    public static double slack(AtcTask task, double t) {
        double d = task.dueHours() != null ? task.dueHours() : Double.POSITIVE_INFINITY;
        return d - task.processingHours() - t;
    }

    public static double urgency(double slack, double pAverage, AtcConfig config) {
        if (slack <= 0.0) return 1.0;
        return Math.exp(-slack / (config.k() * pAverage));
    }

    public static double compute(AtcTask task, double t, double pAverage, AtcConfig config) {
        double w = config.weightOf(task.priority());
        double p = task.processingHours();
        double urgency = urgency(slack(task, t), pAverage, config);
        return (w / p) * urgency;
    }
}
