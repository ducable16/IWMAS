package com.iwas.recommendation.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.recommendation.dto.TfnComparison;
import com.iwas.recommendation.service.FuzzyAhpService.Tfn;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FuzzyAhpServiceTest {

    private final FuzzyAhpService service = new FuzzyAhpService();

    private static TfnComparison tfn(double l, double m, double u) {
        return new TfnComparison(
                BigDecimal.valueOf(l),
                BigDecimal.valueOf(m),
                BigDecimal.valueOf(u));
    }

    // ─── Chang's extent ───────────────────────────────────────────────────────

    @Test
    void chang_equalImportance3x3_givesUniformWeights() {
        TfnComparison eq = tfn(1, 1, 1);
        Tfn[][] m = service.buildTopMatrix(eq, eq, eq);
        double[] w = service.chang(m);

        assertEquals(3, w.length);
        for (double v : w) assertEquals(1.0 / 3.0, v, 0.001);
    }

    @Test
    void chang_2x2_strongerSideGetsHigherWeight() {
        // coverageVsLevel = (1, 2, 3) → coverage about 2× more important
        Tfn[][] m = service.buildSkillMatrix(tfn(1, 2, 3));
        double[] w = service.chang(m);

        assertEquals(2, w.length);
        assertTrue(w[0] > w[1],
                "Coverage should outweigh Level; got w=" + w[0] + " vs " + w[1]);
        assertEquals(1.0, w[0] + w[1], 0.001);
    }

    @Test
    void chang_weightsSumToOne() {
        Tfn[][] m = service.buildTopMatrix(tfn(2, 3, 4), tfn(4, 5, 6), tfn(1, 2, 3));
        double[] w = service.chang(m);
        double sum = 0;
        for (double v : w) sum += v;
        assertEquals(1.0, sum, 0.001);
    }

    @Test
    void chang_dominantCriterionGetsHighestWeight() {
        // Skill very strong vs Workload and vs On-time
        Tfn[][] m = service.buildTopMatrix(tfn(4, 5, 6), tfn(5, 6, 7), tfn(1, 1, 2));
        double[] w = service.chang(m);

        assertTrue(w[0] > w[1], "Skill > Workload, got " + w[0] + " vs " + w[1]);
        assertTrue(w[0] > w[2], "Skill > On-time, got " + w[0] + " vs " + w[2]);
    }

    // ─── Consistency Ratio ────────────────────────────────────────────────────

    @Test
    void consistencyRatio_2x2_isAlwaysZero() {
        Tfn[][] m = service.buildSkillMatrix(tfn(1, 5, 9));
        assertEquals(0.0, service.consistencyRatio(m), 0.0001);
    }

    @Test
    void consistencyRatio_perfectlyConsistent3x3_isNearZero() {
        // Transitive: skill.m × workload.m = ontime.m → 3 × 2 = 6 ✓
        Tfn[][] m = service.buildTopMatrix(tfn(2, 3, 4), tfn(5, 6, 7), tfn(1, 2, 3));
        double cr = service.consistencyRatio(m);
        assertTrue(cr < 0.05,
                "Consistent matrix should give CR < 0.05, got " + cr);
    }

    @Test
    void consistencyRatio_inconsistent3x3_exceedsThreshold() {
        // sw.m × wo.m = 15 but so.m = 2 → very inconsistent (admin contradicts themselves)
        Tfn[][] m = service.buildTopMatrix(tfn(2, 3, 4), tfn(1, 2, 3), tfn(4, 5, 6));
        double cr = service.consistencyRatio(m);
        assertTrue(cr > 0.1,
                "Inconsistent matrix should give CR > 0.1, got " + cr);
    }

    // ─── validation ───────────────────────────────────────────────────────────

    @Test
    void validateTfn_rejectsOrderViolation() {
        AppException ex = assertThrows(AppException.class,
                () -> service.validateTfn(tfn(3, 2, 4)));  // l > m
        assertEquals(ErrorCode.AHP_INVALID_TFN, ex.getErrorCode());
    }

    @Test
    void validateTfn_rejectsValueAboveScale() {
        AppException ex = assertThrows(AppException.class,
                () -> service.validateTfn(tfn(1, 5, 10)));  // u > 9
        assertEquals(ErrorCode.AHP_INVALID_TFN, ex.getErrorCode());
    }

    @Test
    void validateTfn_acceptsValidInput() {
        assertDoesNotThrow(() -> service.validateTfn(tfn(1, 2, 3)));
        assertDoesNotThrow(() -> service.validateTfn(tfn(1, 1, 1)));
    }
}
