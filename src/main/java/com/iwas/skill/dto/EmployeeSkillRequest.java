package com.iwas.skill.dto;

import com.iwas.skill.enums.SkillLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmployeeSkillRequest {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    @NotNull(message = "Skill level is required")
    private SkillLevel level;

    private BigDecimal yearsOfExperience;

    private String note;
}
