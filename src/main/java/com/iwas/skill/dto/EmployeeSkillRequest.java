package com.iwas.skill.dto;

import com.iwas.skill.enums.SkillLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeeSkillRequest {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    @NotNull(message = "Skill level is required")
    private SkillLevel level;

    private String note;
}
