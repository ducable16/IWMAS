package com.iwas.arrangement.model;

import com.iwas.arrangement.config.AtcProperties;
import com.iwas.task.enums.TaskPriority;

import java.util.EnumMap;
import java.util.Map;

public class AtcConfig {

    private static AtcConfig defaultConfig;

    private final Map<TaskPriority, Double> weights;
    private final double k;

    public AtcConfig(Map<TaskPriority, Double> weights, double k) {
        this.weights = weights;
        this.k = k;
    }

    public static AtcConfig getDefault() {
        if (defaultConfig == null) {
            throw new IllegalStateException(
                    "AtcConfig.defaultConfig is not initialized yet — accessed before Spring startup completed");
        }
        return defaultConfig;
    }

    public static void initialize(AtcProperties props) {
        defaultConfig = new AtcConfig(new EnumMap<>(props.getWeights()), props.getK());
    }

    public Map<TaskPriority, Double> weights() {
        return weights;
    }

    public double k() {
        return k;
    }

    public AtcConfig withOverrides(Double kOverride, Map<TaskPriority, Double> weightOverrides) {
        Map<TaskPriority, Double> merged = new EnumMap<>(weights);
        if (weightOverrides != null) merged.putAll(weightOverrides);
        return new AtcConfig(merged, kOverride != null ? kOverride : k);
    }

    public double weightOf(TaskPriority priority) {
        return weights.get(priority);
    }
}
