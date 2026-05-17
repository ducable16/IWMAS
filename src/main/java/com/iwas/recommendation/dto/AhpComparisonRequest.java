package com.iwas.recommendation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Four pairwise comparisons that fully describe the AHP hierarchy:
 * <ul>
 *   <li>3 pairs for the top-level 3×3 matrix (Skill / Workload / On-time)</li>
 *   <li>1 pair for the Skill 2×2 sub-matrix (Coverage / Level)</li>
 * </ul>
 * Each TFN is read as "how much more important is {@code left} than {@code right}".
 */
@Data
public class AhpComparisonRequest {

    @NotNull
    @Valid
    private TfnComparison skillVsWorkload;

    @NotNull
    @Valid
    private TfnComparison skillVsOnTime;

    @NotNull
    @Valid
    private TfnComparison workloadVsOnTime;

    @NotNull
    @Valid
    private TfnComparison coverageVsLevel;

    private String note;
}
