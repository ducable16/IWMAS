package com.iwas.project.service;

import com.iwas.common.exception.AppException;
import com.iwas.project.dto.ProjectCodeSuggestResponse;
import com.iwas.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectCodeServiceTest {

    @Mock
    private ProjectRepository repo;

    @Spy
    private ProjectCodeGenerator generator = new ProjectCodeGenerator();

    @InjectMocks
    private ProjectCodeService service;

    // ── suggest: always returns a guaranteed-unique code ─────────────────────

    @Test
    void suggest_baseAvailable_returnsBase() {
        when(repo.existsByCodeIgnoreCase("HRM")).thenReturn(false);
        ProjectCodeSuggestResponse resp = service.suggest("Human Resource Management");
        assertEquals("HRM", resp.getCode());
    }

    @Test
    void suggest_baseTaken_returnsSuffixedUnique() {
        when(repo.existsByCodeIgnoreCase("HRM")).thenReturn(true);
        when(repo.existsByCodeIgnoreCase("HRM-2")).thenReturn(false);
        ProjectCodeSuggestResponse resp = service.suggest("Human Resource Management");
        assertEquals("HRM-2", resp.getCode());
    }

    @Test
    void suggest_multipleConflicts_returnsFirstFree() {
        when(repo.existsByCodeIgnoreCase("HRM")).thenReturn(true);
        when(repo.existsByCodeIgnoreCase("HRM-2")).thenReturn(true);
        when(repo.existsByCodeIgnoreCase("HRM-3")).thenReturn(false);
        assertEquals("HRM-3", service.suggest("Human Resource Management").getCode());
    }

    // ── normalize ────────────────────────────────────────────────────────────

    @Test
    void normalize_lowercaseToUpper() {
        assertEquals("MYPROJ", service.normalize("myproj"));
    }

    @Test
    void normalize_stripsInvalidChars() {
        assertEquals("MYPROJ", service.normalize("  my proj!! "));
    }

    @Test
    void normalize_preservesHyphen() {
        assertEquals("HRM-2", service.normalize("hrm-2"));
    }

    // ── validate ─────────────────────────────────────────────────────────────

    @Test
    void validate_valid_passes() {
        assertDoesNotThrow(() -> service.validate("HRM"));
        assertDoesNotThrow(() -> service.validate("HRM-2"));
        assertDoesNotThrow(() -> service.validate("AB"));
        assertDoesNotThrow(() -> service.validate("ABCDEFGHIJ")); // exactly 10
    }

    @Test
    void validate_tooShort_throws() {
        assertThrows(AppException.class, () -> service.validate("A"));
    }

    @Test
    void validate_tooLong_throws() {
        assertThrows(AppException.class, () -> service.validate("ABCDEFGHIJK")); // 11 chars
    }

    @Test
    void validate_lowercaseChars_throws() {
        assertThrows(AppException.class, () -> service.validate("hrm"));
    }

    @Test
    void validate_specialChars_throws() {
        assertThrows(AppException.class, () -> service.validate("HRM!"));
    }

    // ── resolveForCreate: no code provided → auto-suffix ─────────────────────

    @Test
    void resolveForCreate_noCode_generatesUnique() {
        when(repo.existsByCodeIgnoreCase("HRM")).thenReturn(false);
        assertEquals("HRM", service.resolveForCreate(null, "Human Resource Management"));
    }

    @Test
    void resolveForCreate_noCode_baseTaken_autoSuffix() {
        when(repo.existsByCodeIgnoreCase("HRM")).thenReturn(true);
        when(repo.existsByCodeIgnoreCase("HRM-2")).thenReturn(false);
        assertEquals("HRM-2", service.resolveForCreate(null, "Human Resource Management"));
    }

    @Test
    void resolveForCreate_noCode_multipleConflicts() {
        when(repo.existsByCodeIgnoreCase("HRM")).thenReturn(true);
        when(repo.existsByCodeIgnoreCase("HRM-2")).thenReturn(true);
        when(repo.existsByCodeIgnoreCase("HRM-3")).thenReturn(false);
        assertEquals("HRM-3", service.resolveForCreate(null, "Human Resource Management"));
    }

    // ── resolveForCreate: user provides code → strict, no auto-suffix ────────

    @Test
    void resolveForCreate_userCode_available_returns() {
        when(repo.existsByCodeIgnoreCase("MYPROJ")).thenReturn(false);
        assertEquals("MYPROJ", service.resolveForCreate("myproj", "anything"));
    }

    @Test
    void resolveForCreate_userCode_taken_throws409() {
        when(repo.existsByCodeIgnoreCase("HRM")).thenReturn(true);
        assertThrows(AppException.class,
                () -> service.resolveForCreate("HRM", "Human Resource Management"));
    }

    @Test
    void resolveForCreate_userCode_invalidFormat_throws() {
        assertThrows(AppException.class,
                () -> service.resolveForCreate("invalid code!", "anything"));
    }

    @Test
    void resolveForCreate_longBase_truncatedBeforeSuffix() {
        // 8-char base taken → truncate to 7 + "-2" = 9 chars ≤ 10
        when(repo.existsByCodeIgnoreCase("ABCDEFGH")).thenReturn(true);
        when(repo.existsByCodeIgnoreCase("ABCDEFG-2")).thenReturn(false);
        // user did NOT provide a code, system generates from name mocked via spy
        doReturn("ABCDEFGH").when(generator).generate("anything");
        String result = service.resolveForCreate(null, "anything");
        assertEquals("ABCDEFG-2", result);
        assertTrue(result.length() <= 10);
    }
}
