package com.iwas.arrangement.model;

import com.iwas.arrangement.config.AtcProperties;
import com.iwas.task.enums.TaskPriority;

import java.util.EnumMap;
import java.util.Map;

public class AtcConfig {

    private static AtcConfig defaultConfig;

    private final Map<TaskPriority, Double> weights;
    private final double k;
    private final TaskPriority priorityFallback;

    public AtcConfig(Map<TaskPriority, Double> weights, double k, TaskPriority priorityFallback) {
        this.weights = weights;
        this.k = k;
        this.priorityFallback = priorityFallback;
    }

    public static AtcConfig getDefault() {
        if (defaultConfig == null) {
            throw new IllegalStateException(
                    "AtcConfig.defaultConfig is not initialized yet — accessed before Spring startup completed");
        }
        return defaultConfig;
    }

    public static void initialize(AtcProperties props) {
        defaultConfig = new AtcConfig(new EnumMap<>(props.getWeights()), props.getK(), props.getPriorityFallback());
    }

    public Map<TaskPriority, Double> weights() {
        return weights;
    }

    public double k() {
        return k;
    }

    public TaskPriority priorityFallback() {
        return priorityFallback;
    }

    /**
     * Returns a copy with the supplied overrides applied. Null arguments leave
     * the corresponding parameter unchanged; weight overrides are merged on top
     * of the existing table (only the supplied priorities change).
     */
    public AtcConfig withOverrides(Double kOverride, Map<TaskPriority, Double> weightOverrides) {
        Map<TaskPriority, Double> merged = new EnumMap<>(weights);
        if (weightOverrides != null) merged.putAll(weightOverrides);
        return new AtcConfig(merged, kOverride != null ? kOverride : k, priorityFallback);
    }

    /** Weight {@code wⱼ} for a priority, falling back to {@link #priorityFallback}. */
    public double weightOf(TaskPriority priority) {
        Double w = priority != null ? weights.get(priority) : null;
        if (w != null) return w;
        Double fallback = weights.get(priorityFallback);
        return fallback != null ? fallback : 1.0;
    }
}
