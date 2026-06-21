package com.iwas.workload.dto;

import com.iwas.workload.enums.WorkloadLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Effect of assigning a hypothetical task to a candidate's project lane —
 * a "what-if" the assignee recommender can score availability with.
 *
 * Computed by re-running the schedule simulation with the candidate task
 * appended; nothing is persisted.
 */
@Getter
@Builder
public class CandidateWorkloadImpact {
    private Long userId;
    private Long projectId;
    private WorkloadLevel levelBefore;
    private WorkloadLevel levelAfter;
    private BigDecimal workloadPercentBefore;
    private BigDecimal workloadPercentAfter;
    private Integer predictedLateTaskCountAfter;
    /** The candidate task itself is projected to miss its own due date. */
    private boolean candidateTaskWillSlip;
    /** Assigning the task makes at least one more task slip than before. */
    private boolean introducesNewSlip;
}
