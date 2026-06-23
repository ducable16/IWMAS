package com.iwas.workload.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Effect of assigning a hypothetical task to a candidate's project lane —
 * a "what-if" the assignee recommender can score availability with.
 *
 * Computed by re-running the schedule simulation with the candidate task
 * appended; nothing is persisted. {@code loadLevel*}/{@code backlogDays*} are the
 * volume axis before/after; the slip fields are the deadline-risk axis.
 */
@Getter
@Builder
public class CandidateWorkloadImpact {
    private Long userId;
    private Long projectId;
    private BigDecimal backlogDaysBefore;
    private BigDecimal backlogDaysAfter;
    private Integer predictedLateTaskCountAfter;
    /** The candidate task itself is projected to miss its own due date. */
    private boolean candidateTaskWillSlip;
    /** Assigning the task makes at least one more task slip than before. */
    private boolean introducesNewSlip;
}
