package com.roamtrip.task.dto;

import com.roamtrip.skill.enums.SkillLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskSkillRequirementResponse {
    private Long id;
    private Long skillId;
    private String skillName;
    private SkillLevel minimumLevel;
    private Boolean isRequired;
}
