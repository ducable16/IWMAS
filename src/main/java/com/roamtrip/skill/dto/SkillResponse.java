package com.roamtrip.skill.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillResponse {
    private Long id;
    private String name;
    private String category;
    private String description;
}
