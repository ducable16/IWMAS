package com.iwas.skill.controller;

import com.iwas.skill.dto.SkillCategoryRequest;
import com.iwas.skill.dto.SkillCategoryResponse;
import com.iwas.skill.service.SkillCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skill-categories")
@RequiredArgsConstructor
public class SkillCategoryController {

    private final SkillCategoryService skillCategoryService;

    @GetMapping
    public List<SkillCategoryResponse> getAll() {
        return skillCategoryService.getAll();
    }

    @GetMapping("/{id}")
    public SkillCategoryResponse getById(@PathVariable Long id) {
        return skillCategoryService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillCategoryResponse create(@Valid @RequestBody SkillCategoryRequest request) {
        return skillCategoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SkillCategoryResponse update(@PathVariable Long id,
                                        @Valid @RequestBody SkillCategoryRequest request) {
        return skillCategoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        skillCategoryService.delete(id);
    }
}
