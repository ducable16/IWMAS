package com.iwas.recommendation.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.recommendation.dto.AhpComparisonRequest;
import com.iwas.recommendation.dto.AhpWeightResponse;
import com.iwas.recommendation.dto.TfnComparison;
import com.iwas.recommendation.entity.AhpWeightSet;
import com.iwas.recommendation.repository.AhpWeightSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AhpWeightServiceTest {

    @Mock
    AhpWeightSetRepository repo;

    @Spy
    FuzzyAhpService fuzzyAhp = new FuzzyAhpService();

    @InjectMocks
    AhpWeightService service;

    private static TfnComparison tfn(double l, double m, double u) {
        return new TfnComparison(
                BigDecimal.valueOf(l),
                BigDecimal.valueOf(m),
                BigDecimal.valueOf(u));
    }

    /** Transitively-consistent request: sw.m × wo.m = so.m → 3 × 2 = 6. */
    private AhpComparisonRequest consistentRequest() {
        AhpComparisonRequest r = new AhpComparisonRequest();
        r.setSkillVsWorkload(tfn(2, 3, 4));
        r.setSkillVsOnTime(tfn(5, 6, 7));
        r.setWorkloadVsOnTime(tfn(1, 2, 3));
        r.setCoverageVsLevel(tfn(1, 2, 3));
        r.setNote("consistent test");
        return r;
    }

    /** Intentionally inconsistent: sw.m × wo.m = 15, but so.m = 2. */
    private AhpComparisonRequest inconsistentRequest() {
        AhpComparisonRequest r = new AhpComparisonRequest();
        r.setSkillVsWorkload(tfn(2, 3, 4));
        r.setSkillVsOnTime(tfn(1, 2, 3));
        r.setWorkloadVsOnTime(tfn(4, 5, 6));
        r.setCoverageVsLevel(tfn(1, 1, 1));
        return r;
    }

    private AhpWeightSet existingEntity(long id, int version, boolean active) {
        AhpWeightSet e = new AhpWeightSet();
        e.setId(id);
        e.setVersion(version);
        e.setIsActive(active);
        e.setIsDeleted(false);
        e.setWeightSkillCoverage(new BigDecimal("0.3"));
        e.setWeightSkillLevel(new BigDecimal("0.2"));
        e.setWeightWorkload(new BigDecimal("0.35"));
        e.setWeightOntime(new BigDecimal("0.15"));
        e.setSkillWorkloadL(BigDecimal.ONE);  e.setSkillWorkloadM(BigDecimal.ONE);  e.setSkillWorkloadU(BigDecimal.ONE);
        e.setSkillOntimeL(BigDecimal.ONE);    e.setSkillOntimeM(BigDecimal.ONE);    e.setSkillOntimeU(BigDecimal.ONE);
        e.setWorkloadOntimeL(BigDecimal.ONE); e.setWorkloadOntimeM(BigDecimal.ONE); e.setWorkloadOntimeU(BigDecimal.ONE);
        e.setCoverageLevelL(BigDecimal.ONE);  e.setCoverageLevelM(BigDecimal.ONE);  e.setCoverageLevelU(BigDecimal.ONE);
        e.setConsistencyRatio(new BigDecimal("0.05"));
        e.setIsConsistent(true);
        return e;
    }

    // ─── submit ───────────────────────────────────────────────────────────────

    @Test
    void submit_firstTime_savesAsVersion1Active() {
        when(repo.findActive()).thenReturn(Optional.empty());
        when(repo.findTopByOrderByVersionDesc()).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> {
            AhpWeightSet e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        AhpWeightResponse resp = service.submit(consistentRequest());

        assertEquals(1, resp.getVersion());
        assertTrue(resp.getIsActive());
        assertNotNull(resp.getWeights().getSkillCoverage());
        verify(repo, times(1)).save(any(AhpWeightSet.class));
    }

    @Test
    void submit_secondTime_deactivatesPreviousAndIncrementsVersion() {
        AhpWeightSet prev = existingEntity(5L, 3, true);
        when(repo.findActive()).thenReturn(Optional.of(prev));
        when(repo.findTopByOrderByVersionDesc()).thenReturn(Optional.of(prev));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AhpWeightResponse resp = service.submit(consistentRequest());

        assertEquals(4, resp.getVersion());
        assertTrue(resp.getIsActive());
        assertFalse(prev.getIsActive(), "previous version should be deactivated");
        verify(repo, times(2)).save(any(AhpWeightSet.class)); // deactivate + new
    }

    @Test
    void submit_inconsistentMatrix_stillSavesWithFlagFalse() {
        when(repo.findActive()).thenReturn(Optional.empty());
        when(repo.findTopByOrderByVersionDesc()).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AhpWeightResponse resp = service.submit(inconsistentRequest());

        assertFalse(resp.getIsConsistent(),
                "inconsistent matrix should still save with isConsistent=false");
        assertTrue(resp.getConsistencyRatio().compareTo(new BigDecimal("0.10")) >= 0,
                "expected CR >= 0.10, got " + resp.getConsistencyRatio());
        verify(repo, times(1)).save(any(AhpWeightSet.class));
    }

    @Test
    void submit_invalidTfn_throwsAndDoesNotSave() {
        AhpComparisonRequest r = consistentRequest();
        r.setSkillVsWorkload(tfn(4, 2, 3));  // l > m → invalid

        AppException ex = assertThrows(AppException.class, () -> service.submit(r));
        assertEquals(ErrorCode.AHP_INVALID_TFN, ex.getErrorCode());
        verify(repo, never()).save(any());
    }

    // ─── getActive / getById ──────────────────────────────────────────────────

    @Test
    void getActive_noVersionConfigured_throws2003() {
        when(repo.findActive()).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getActive());
        assertEquals(ErrorCode.AHP_NO_ACTIVE_WEIGHT_SET, ex.getErrorCode());
    }

    @Test
    void getActive_returnsActiveVersion() {
        AhpWeightSet active = existingEntity(7L, 5, true);
        when(repo.findActive()).thenReturn(Optional.of(active));

        AhpWeightResponse resp = service.getActive();

        assertEquals(5, resp.getVersion());
        assertTrue(resp.getIsActive());
    }

    @Test
    void getById_notFound_throws2001() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getById(99L));
        assertEquals(ErrorCode.AHP_WEIGHT_SET_NOT_FOUND, ex.getErrorCode());
    }

    // ─── activate ─────────────────────────────────────────────────────────────

    @Test
    void activate_swapsActiveFromCurrentToTarget() {
        AhpWeightSet target = existingEntity(2L, 2, false);
        AhpWeightSet current = existingEntity(3L, 3, true);
        when(repo.findById(2L)).thenReturn(Optional.of(target));
        when(repo.findActive()).thenReturn(Optional.of(current));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AhpWeightResponse resp = service.activate(2L);

        assertTrue(resp.getIsActive());
        assertEquals(2, resp.getVersion());
        assertFalse(current.getIsActive(), "previous active should be deactivated");
    }

    @Test
    void activate_nonexistentId_throws2001() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.activate(99L));
        assertEquals(ErrorCode.AHP_WEIGHT_SET_NOT_FOUND, ex.getErrorCode());
    }
}
