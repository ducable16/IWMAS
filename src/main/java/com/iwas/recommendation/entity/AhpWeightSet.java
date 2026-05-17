package com.iwas.recommendation.entity;

import com.iwas.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Stores one version of the AHP weight set together with the raw fuzzy
 * pairwise inputs that produced it. Versions are append-only; rollback is done
 * by toggling {@link BaseEntity#getIsActive()} on the desired row (the
 * service guarantees at most one row has {@code isActive = true} at any time).
 *
 * <p>Twelve {@code *_l/m/u} columns store the four TFN inputs the admin
 * submitted. They are kept for audit and to repopulate the form when the admin
 * re-edits a past version.
 */
@Getter
@Setter
@Entity
@Table(
        name = "ahp_weight_sets",
        indexes = {
                @Index(name = "idx_ahp_active", columnList = "is_active"),
                @Index(name = "idx_ahp_version", columnList = "version")
        }
)
public class AhpWeightSet extends BaseEntity {

    @Column(nullable = false)
    private Integer version;

    // ─── computed leaf weights (sum = 1.0000) ────────────────────────────────

    @Column(name = "weight_skill_coverage", precision = 6, scale = 4, nullable = false)
    private BigDecimal weightSkillCoverage;

    @Column(name = "weight_skill_level", precision = 6, scale = 4, nullable = false)
    private BigDecimal weightSkillLevel;

    @Column(name = "weight_workload", precision = 6, scale = 4, nullable = false)
    private BigDecimal weightWorkload;

    @Column(name = "weight_ontime", precision = 6, scale = 4, nullable = false)
    private BigDecimal weightOntime;

    // ─── consistency check ───────────────────────────────────────────────────

    @Column(name = "consistency_ratio", precision = 6, scale = 4)
    private BigDecimal consistencyRatio;

    @Column(name = "is_consistent")
    private Boolean isConsistent;

    // ─── raw TFN inputs (4 pairs × 3 components) ─────────────────────────────

    @Column(name = "skill_workload_l", precision = 6, scale = 4, nullable = false)
    private BigDecimal skillWorkloadL;
    @Column(name = "skill_workload_m", precision = 6, scale = 4, nullable = false)
    private BigDecimal skillWorkloadM;
    @Column(name = "skill_workload_u", precision = 6, scale = 4, nullable = false)
    private BigDecimal skillWorkloadU;

    @Column(name = "skill_ontime_l", precision = 6, scale = 4, nullable = false)
    private BigDecimal skillOntimeL;
    @Column(name = "skill_ontime_m", precision = 6, scale = 4, nullable = false)
    private BigDecimal skillOntimeM;
    @Column(name = "skill_ontime_u", precision = 6, scale = 4, nullable = false)
    private BigDecimal skillOntimeU;

    @Column(name = "workload_ontime_l", precision = 6, scale = 4, nullable = false)
    private BigDecimal workloadOntimeL;
    @Column(name = "workload_ontime_m", precision = 6, scale = 4, nullable = false)
    private BigDecimal workloadOntimeM;
    @Column(name = "workload_ontime_u", precision = 6, scale = 4, nullable = false)
    private BigDecimal workloadOntimeU;

    @Column(name = "coverage_level_l", precision = 6, scale = 4, nullable = false)
    private BigDecimal coverageLevelL;
    @Column(name = "coverage_level_m", precision = 6, scale = 4, nullable = false)
    private BigDecimal coverageLevelM;
    @Column(name = "coverage_level_u", precision = 6, scale = 4, nullable = false)
    private BigDecimal coverageLevelU;

    // ─── note ────────────────────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String note;
}
