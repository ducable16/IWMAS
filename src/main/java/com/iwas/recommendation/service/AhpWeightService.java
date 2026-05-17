package com.iwas.recommendation.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.recommendation.dto.AhpComparisonRequest;
import com.iwas.recommendation.dto.AhpWeightResponse;
import com.iwas.recommendation.dto.TfnComparison;
import com.iwas.recommendation.entity.AhpWeightSet;
import com.iwas.recommendation.repository.AhpWeightSetRepository;
import com.iwas.recommendation.service.FuzzyAhpService.Tfn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Orchestrates AHP weight-set lifecycle: validation → matrix build →
 * Chang's extent + Consistency Ratio → persist with versioning.
 *
 * <p>Invariants enforced here:
 * <ul>
 *   <li>At most one row has {@code isActive = true} at any time.</li>
 *   <li>{@code version} is strictly increasing (assigned by service, not DB seq).</li>
 *   <li>Leaf weights are stored rounded to 4 decimal places; they may not sum
 *       to exactly 1.0000 after rounding but stay within 0.0002 tolerance.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AhpWeightService {

    private final AhpWeightSetRepository repository;
    private final FuzzyAhpService fuzzyAhp;

    private static final BigDecimal CR_THRESHOLD = new BigDecimal("0.10");
    private static final int WEIGHT_SCALE = 4;

    // ─── public API ───────────────────────────────────────────────────────────

    @Transactional
    public AhpWeightResponse submit(AhpComparisonRequest request) {
        fuzzyAhp.validateTfn(request.getSkillVsWorkload());
        fuzzyAhp.validateTfn(request.getSkillVsOnTime());
        fuzzyAhp.validateTfn(request.getWorkloadVsOnTime());
        fuzzyAhp.validateTfn(request.getCoverageVsLevel());

        Tfn[][] topMatrix = fuzzyAhp.buildTopMatrix(
                request.getSkillVsWorkload(),
                request.getSkillVsOnTime(),
                request.getWorkloadVsOnTime());
        Tfn[][] skillMatrix = fuzzyAhp.buildSkillMatrix(request.getCoverageVsLevel());

        double[] topWeights = fuzzyAhp.chang(topMatrix);     // [skill, workload, ontime]
        double[] skillWeights = fuzzyAhp.chang(skillMatrix); // [coverage, level]

        double wSkill = topWeights[0];
        double wWorkload = topWeights[1];
        double wOnTime = topWeights[2];
        double wCoverage = wSkill * skillWeights[0];
        double wLevel = wSkill * skillWeights[1];

        double cr = fuzzyAhp.consistencyRatio(topMatrix);
        BigDecimal crRounded = BigDecimal.valueOf(cr).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
        boolean consistent = crRounded.compareTo(CR_THRESHOLD) < 0;

        // Deactivate the currently-active version (if any)
        repository.findActive().ifPresent(existing -> {
            existing.setIsActive(false);
            repository.save(existing);
        });

        int nextVersion = repository.findTopByOrderByVersionDesc()
                .map(latest -> latest.getVersion() + 1)
                .orElse(1);

        AhpWeightSet entity = new AhpWeightSet();
        entity.setVersion(nextVersion);
        entity.setIsActive(true);
        entity.setWeightSkillCoverage(round(wCoverage));
        entity.setWeightSkillLevel(round(wLevel));
        entity.setWeightWorkload(round(wWorkload));
        entity.setWeightOntime(round(wOnTime));
        entity.setConsistencyRatio(crRounded);
        entity.setIsConsistent(consistent);
        copyRawTfn(entity, request);
        entity.setNote(request.getNote());

        return toResponse(repository.save(entity));
    }

    public AhpWeightResponse getActive() {
        AhpWeightSet active = repository.findActive()
                .orElseThrow(() -> new AppException(ErrorCode.AHP_NO_ACTIVE_WEIGHT_SET));
        return toResponse(active);
    }

    public List<AhpWeightResponse> getAll() {
        return repository.findAllByOrderByVersionDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public AhpWeightResponse getById(Long id) {
        AhpWeightSet entity = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.AHP_WEIGHT_SET_NOT_FOUND));
        return toResponse(entity);
    }

    @Transactional
    public AhpWeightResponse activate(Long id) {
        AhpWeightSet target = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.AHP_WEIGHT_SET_NOT_FOUND));

        repository.findActive().ifPresent(currentActive -> {
            if (!currentActive.getId().equals(id)) {
                currentActive.setIsActive(false);
                repository.save(currentActive);
            }
        });

        target.setIsActive(true);
        return toResponse(repository.save(target));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }

    private void copyRawTfn(AhpWeightSet e, AhpComparisonRequest r) {
        TfnComparison sw = r.getSkillVsWorkload();
        e.setSkillWorkloadL(sw.getL());
        e.setSkillWorkloadM(sw.getM());
        e.setSkillWorkloadU(sw.getU());

        TfnComparison so = r.getSkillVsOnTime();
        e.setSkillOntimeL(so.getL());
        e.setSkillOntimeM(so.getM());
        e.setSkillOntimeU(so.getU());

        TfnComparison wo = r.getWorkloadVsOnTime();
        e.setWorkloadOntimeL(wo.getL());
        e.setWorkloadOntimeM(wo.getM());
        e.setWorkloadOntimeU(wo.getU());

        TfnComparison cl = r.getCoverageVsLevel();
        e.setCoverageLevelL(cl.getL());
        e.setCoverageLevelM(cl.getM());
        e.setCoverageLevelU(cl.getU());
    }

    private AhpWeightResponse toResponse(AhpWeightSet e) {
        BigDecimal skillParent = e.getWeightSkillCoverage().add(e.getWeightSkillLevel());

        return AhpWeightResponse.builder()
                .id(e.getId())
                .version(e.getVersion())
                .isActive(e.getIsActive())
                .weights(AhpWeightResponse.Weights.builder()
                        .skillCoverage(e.getWeightSkillCoverage())
                        .skillLevel(e.getWeightSkillLevel())
                        .workload(e.getWeightWorkload())
                        .onTime(e.getWeightOntime())
                        .build())
                .skillParentWeight(skillParent)
                .fuzzyMatrix(AhpWeightResponse.FuzzyMatrix.builder()
                        .skillVsWorkload(new TfnComparison(e.getSkillWorkloadL(), e.getSkillWorkloadM(), e.getSkillWorkloadU()))
                        .skillVsOnTime(new TfnComparison(e.getSkillOntimeL(), e.getSkillOntimeM(), e.getSkillOntimeU()))
                        .workloadVsOnTime(new TfnComparison(e.getWorkloadOntimeL(), e.getWorkloadOntimeM(), e.getWorkloadOntimeU()))
                        .coverageVsLevel(new TfnComparison(e.getCoverageLevelL(), e.getCoverageLevelM(), e.getCoverageLevelU()))
                        .build())
                .consistencyRatio(e.getConsistencyRatio())
                .isConsistent(e.getIsConsistent())
                .note(e.getNote())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
