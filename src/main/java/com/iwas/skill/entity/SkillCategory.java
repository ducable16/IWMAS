package com.iwas.skill.entity;

import com.iwas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "skill_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_skill_category_name", columnNames = {"name"})
        }
)
public class SkillCategory extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
