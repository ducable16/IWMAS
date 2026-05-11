package com.iwas.project.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.project.dto.ProjectCodeSuggestResponse;
import com.iwas.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProjectCodeService {

    private static final Pattern VALID_CODE = Pattern.compile("^[A-Z0-9-]{2,10}$");
    private static final int MAX_SUFFIX = 99;

    private final ProjectRepository projectRepository;
    private final ProjectCodeGenerator generator;

    /** Returns a guaranteed-unique code derived from the project name. */
    public ProjectCodeSuggestResponse suggest(String name) {
        String code = findUniqueCode(generator.generate(name));
        return ProjectCodeSuggestResponse.builder()
                .code(code)
                .build();
    }

    public String normalize(String raw) {
        return raw.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "");
    }

    public void validate(String code) {
        if (!VALID_CODE.matcher(code).matches()) {
            throw new AppException(ErrorCode.PROJECT_CODE_INVALID);
        }
    }

    /**
     * Resolves the final code to save:
     * - User provided a code → normalize + validate + strict uniqueness check (no auto-suffix).
     * - No code provided    → auto-generate from name, auto-suffix until unique.
     */
    public String resolveForCreate(String requestedCode, String projectName) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            String normalized = normalize(requestedCode);
            validate(normalized);
            if (projectRepository.existsByCodeIgnoreCase(normalized)) {
                throw new AppException(ErrorCode.PROJECT_CODE_ALREADY_EXISTS);
            }
            return normalized;
        }
        return findUniqueCode(generator.generate(projectName));
    }

    /** Finds the first available code starting from base, appending -2, -3, … if needed. */
    private String findUniqueCode(String base) {
        validate(base);
        if (!projectRepository.existsByCodeIgnoreCase(base)) {
            return base;
        }
        String truncated = base.length() > 7 ? base.substring(0, 7) : base;
        for (int i = 2; i <= MAX_SUFFIX; i++) {
            String candidate = truncated + "-" + i;
            if (!projectRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new AppException(ErrorCode.PROJECT_CODE_ALREADY_EXISTS);
    }
}
