package com.iwas.skill.dto;

import com.iwas.skill.enums.SkillLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmployeeSkillResponse {
    private Long id;
    private Long userId;
    private Long skillId;
    private String skillName;
    private String skillCategory;
    private SkillLevel level;
    private BigDecimal yearsOfExperience;
    private String note;
}
