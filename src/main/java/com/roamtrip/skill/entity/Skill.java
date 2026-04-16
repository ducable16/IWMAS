package com.roamtrip.skill.entity;

import com.roamtrip.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "skills",
        indexes = {
                @Index(name = "idx_skill_category", columnList = "category")
        }
)
public class Skill extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
