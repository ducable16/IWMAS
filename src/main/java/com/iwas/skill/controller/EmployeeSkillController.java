package com.iwas.skill.controller;

import com.iwas.security.AuthenticatedUserResolver;
import com.iwas.skill.dto.EmployeeSkillRequest;
import com.iwas.skill.dto.EmployeeSkillResponse;
import com.iwas.skill.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class EmployeeSkillController {

    private final SkillService skillService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/{userId}/skills")
    public List<EmployeeSkillResponse> getEmployeeSkills(@PathVariable Long userId) {
        return skillService.getEmployeeSkills(userId);
    }

    @GetMapping("/me/skills")
    public List<EmployeeSkillResponse> getMySkills() {
        return skillService.getEmployeeSkills(authenticatedUserResolver.currentUserId());
    }

    @PostMapping("/me/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeSkillResponse addMySkill(@Valid @RequestBody EmployeeSkillRequest request) {
        return skillService.addEmployeeSkill(authenticatedUserResolver.currentUserId(), request);
    }

    @PostMapping("/me/skills/{skillId}/update")
    public EmployeeSkillResponse updateMySkill(@PathVariable Long skillId,
                                               @Valid @RequestBody EmployeeSkillRequest request) {
        return skillService.updateEmployeeSkill(authenticatedUserResolver.currentUserId(), skillId, request);
    }

    @PostMapping("/me/skills/{skillId}/delete")
    public void removeMySkill(@PathVariable Long skillId) {
        skillService.removeEmployeeSkill(authenticatedUserResolver.currentUserId(), skillId);
    }

    @PostMapping("/{userId}/skills")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeSkillResponse addEmployeeSkill(@PathVariable Long userId,
                                                  @Valid @RequestBody EmployeeSkillRequest request) {
        return skillService.addEmployeeSkill(userId, request);
    }
}
