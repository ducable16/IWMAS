package com.iwas.recommendation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AhpWeightResponse {

    private Long id;
    private Integer version;
    private Boolean isActive;

    private Weights weights;
    /** Parent weight of the Skill node = skillCoverage + skillLevel. Convenience for the UI. */
    private BigDecimal skillParentWeight;

    /** Echo of the raw TFN inputs that produced this version — useful for the admin form. */
    private FuzzyMatrix fuzzyMatrix;

    private BigDecimal consistencyRatio;
    private Boolean isConsistent;

    private String note;
    private Long createdBy;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class Weights {
        private BigDecimal skillCoverage;
        private BigDecimal skillLevel;
        private BigDecimal workload;
        private BigDecimal onTime;
    }

    @Data
    @Builder
    public static class FuzzyMatrix {
        private TfnComparison skillVsWorkload;
        private TfnComparison skillVsOnTime;
        private TfnComparison workloadVsOnTime;
        private TfnComparison coverageVsLevel;
    }
}
