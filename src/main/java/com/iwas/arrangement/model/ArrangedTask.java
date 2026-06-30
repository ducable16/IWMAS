package com.iwas.arrangement.model;

public record ArrangedTask(Long taskId, int position, double slackHours,
                           double projectedStartHours, double projectedFinishHours,
                           double projectedTardinessHours, boolean estimateDefaulted,
                           String reason) {
}
