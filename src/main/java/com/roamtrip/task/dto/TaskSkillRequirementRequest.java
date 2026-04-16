package com.roamtrip.task.dto;

import com.roamtrip.skill.enums.SkillLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskSkillRequirementRequest {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    private SkillLevel minimumLevel = SkillLevel.INTERMEDIATE;

    private Boolean isRequired = true;
}
