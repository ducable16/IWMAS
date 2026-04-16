package com.roamtrip.skill.controller;

import com.roamtrip.security.AuthenticatedUserResolver;
import com.roamtrip.skill.dto.EmployeeSkillRequest;
import com.roamtrip.skill.dto.EmployeeSkillResponse;
import com.roamtrip.skill.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<EmployeeSkillResponse>> getEmployeeSkills(@PathVariable Long userId) {
        return ResponseEntity.ok(skillService.getEmployeeSkills(userId));
    }

    @GetMapping("/me/skills")
    public ResponseEntity<List<EmployeeSkillResponse>> getMySkills() {
        return ResponseEntity.ok(skillService.getEmployeeSkills(authenticatedUserResolver.currentUserId()));
    }

    @PostMapping("/me/skills")
    public ResponseEntity<EmployeeSkillResponse> addMySkill(@Valid @RequestBody EmployeeSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skillService.addEmployeeSkill(authenticatedUserResolver.currentUserId(), request));
    }

    @PostMapping("/me/skills/{skillId}/update")
    public ResponseEntity<EmployeeSkillResponse> updateMySkill(@PathVariable Long skillId,
                                                               @Valid @RequestBody EmployeeSkillRequest request) {
        return ResponseEntity.ok(skillService.updateEmployeeSkill(authenticatedUserResolver.currentUserId(), skillId, request));
    }

    @PostMapping("/me/skills/{skillId}/delete")
    public ResponseEntity<Void> removeMySkill(@PathVariable Long skillId) {
        skillService.removeEmployeeSkill(authenticatedUserResolver.currentUserId(), skillId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/skills")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<EmployeeSkillResponse> addEmployeeSkill(@PathVariable Long userId,
                                                                  @Valid @RequestBody EmployeeSkillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skillService.addEmployeeSkill(userId, request));
    }
}
