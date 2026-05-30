package com.iwas.skill.controller;

import com.iwas.skill.dto.*;
import com.iwas.skill.enums.SkillLevel;
import com.iwas.skill.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public List<SkillResponse> getAllSkills(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        return skillService.getAllSkills(keyword, categoryId);
    }

    @GetMapping("/{id}")
    public SkillResponse getSkillById(@PathVariable Long id) {
        return skillService.getSkillById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse createSkill(@Valid @RequestBody SkillRequest request) {
        return skillService.createSkill(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SkillResponse updateSkill(@PathVariable Long id,
                                     @Valid @RequestBody SkillRequest request) {
        return skillService.updateSkill(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
    }

    @GetMapping("/{id}/members")
    public List<SkillMemberResponse> getMembersBySkill(
            @PathVariable Long id,
            @RequestParam(required = false) SkillLevel minLevel,
            @RequestParam(required = false) Long excludeProjectId) {
        return skillService.getMembersBySkill(id, minLevel, excludeProjectId);
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'PROJECT_MANAGER')")
    public SkillStatsResponse getSkillStats(@PathVariable Long id) {
        return skillService.getSkillStats(id);
    }
}
