package com.iwas.arrangement.model;

/**
 * One task placed in the suggested execution order, with the data needed to
 * explain <em>why</em> it landed where it did. All time fields are in the
 * abstract work-hours model relative to {@code t0} (the moment arrange was
 * called); the service layer maps them to calendar dates for display.
 *
 * @param taskId                  task identifier
 * @param position                0-based slot in the sequence
 * @param priorityIndex           {@code Iⱼ(t)} at the moment this task was chosen
 * @param slackHours              {@code dⱼ - pⱼ - t} at selection (negative = overdue/at risk)
 * @param projectedStartHours     work-hours from {@code t0} until this task starts
 * @param projectedFinishHours    work-hours from {@code t0} until this task finishes
 * @param projectedTardinessHours projected lateness in hours (0 if on time)
 * @param estimateDefaulted       true when the estimate was missing and a floor was used
 * @param reason                  human-readable explanation (value density + urgency)
 */
public record ArrangedTask(Long taskId, int position, double priorityIndex, double slackHours,
                           double projectedStartHours, double projectedFinishHours,
                           double projectedTardinessHours, boolean estimateDefaulted,
                           String reason) {
}
