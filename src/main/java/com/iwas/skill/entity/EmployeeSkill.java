package com.iwas.skill.entity;

import com.iwas.common.entity.BaseEntity;
import com.iwas.skill.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "employee_skills",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_employee_skill", columnNames = {"user_id", "skill_id"})
        },
        indexes = {
                @Index(name = "idx_employee_skill_user", columnList = "user_id"),
                @Index(name = "idx_employee_skill_skill", columnList = "skill_id")
        }
)
public class EmployeeSkill extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private SkillLevel level;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
