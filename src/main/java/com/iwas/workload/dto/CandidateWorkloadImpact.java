package com.iwas.workload.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CandidateWorkloadImpact {
    private Long userId;
    private Long projectId;
    private BigDecimal backlogDaysBefore;
    private BigDecimal backlogDaysAfter;
    private Integer predictedLateTaskCountAfter;
    private boolean candidateTaskWillSlip;
    private boolean introducesNewSlip;
}
