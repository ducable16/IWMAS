package com.iwas.arrangement.config;

import com.iwas.task.enums.TaskPriority;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.arrangement")
public class AtcProperties {

    /**
     * slides between value-bias (large k → WSPT) and urgency-bias (small k → EDD). Reasonable range 0.5–4.5.
     */
    private double k = 2.0;

    private double minEstimateHours = 1;

    private TaskPriority priorityFallback = TaskPriority.LOW;

    private Map<TaskPriority, Double> weights = defaultWeights();

    private static Map<TaskPriority, Double> defaultWeights() {
        Map<TaskPriority, Double> w = new EnumMap<>(TaskPriority.class);
        w.put(TaskPriority.CRITICAL, 8.0);
        w.put(TaskPriority.HIGH, 4.0);
        w.put(TaskPriority.MEDIUM, 2.0);
        w.put(TaskPriority.LOW, 1.0);
        return w;
    }
}
