package com.iwas.skill.dto;

import com.iwas.skill.enums.SkillLevel;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SkillStatsResponse {
    private Long skillId;
    private String skillName;
    private String skillCategory;
    private long memberCount;
    private Map<SkillLevel, Long> levelDistribution;
    private long openTaskRequirementCount;
}
