package com.iwas.skill.controller;

import com.iwas.skill.dto.*;
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
    public List<SkillResponse> getAllSkills() {
        return skillService.getAllSkills();
    }

    @GetMapping("/{id}")
    public SkillResponse getSkillById(@PathVariable Long id) {
        return skillService.getSkillById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse createSkill(@Valid @RequestBody SkillRequest request) {
        return skillService.createSkill(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
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
}
