package com.iwas.skill.dto;

import com.iwas.skill.enums.SkillLevel;
import com.iwas.user.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillMemberResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String position;
    private UserRole role;
    private SkillLevel level;
    private String note;
}
