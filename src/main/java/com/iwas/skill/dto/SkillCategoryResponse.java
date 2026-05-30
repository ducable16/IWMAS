package com.iwas.skill.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillCategoryResponse {
    private Long id;
    private String name;
    private String description;
}
