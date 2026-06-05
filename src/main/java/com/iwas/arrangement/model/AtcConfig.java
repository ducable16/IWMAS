package com.iwas.arrangement.model;

import com.iwas.arrangement.config.AtcProperties;
import com.iwas.task.enums.TaskPriority;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable, fully-resolved snapshot of the ATC parameters used for a single
 * {@code arrange} call. Built from {@link AtcProperties} and optionally
 * overridden per request (so a grid search / sensitivity analysis can sweep
 * {@code k} and the weights without touching configuration).
 */
public record AtcConfig(Map<TaskPriority, Double> weights, double k,
                        double minEstimateHours, TaskPriority priorityFallback) {

    public static AtcConfig from(AtcProperties props) {
        return new AtcConfig(new EnumMap<>(props.getWeights()), props.getK(),
                props.getMinEstimateHours(), props.getPriorityFallback());
    }

    /**
     * Returns a copy with the supplied overrides applied. Null arguments leave
     * the corresponding parameter unchanged; weight overrides are merged on top
     * of the existing table (only the supplied priorities change).
     */
    public AtcConfig withOverrides(Double kOverride, Map<TaskPriority, Double> weightOverrides) {
        Map<TaskPriority, Double> merged = new EnumMap<>(weights);
        if (weightOverrides != null) merged.putAll(weightOverrides);
        return new AtcConfig(merged, kOverride != null ? kOverride : k,
                minEstimateHours, priorityFallback);
    }

    /** Weight {@code wⱼ} for a priority, falling back to {@link #priorityFallback}. */
    public double weightOf(TaskPriority priority) {
        Double w = priority != null ? weights.get(priority) : null;
        if (w != null) return w;
        Double fallback = weights.get(priorityFallback);
        return fallback != null ? fallback : 1.0;
    }
}
