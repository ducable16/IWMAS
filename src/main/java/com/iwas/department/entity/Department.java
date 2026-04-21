package com.iwas.department.entity;

import com.iwas.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "departments",
        indexes = {
                @Index(name = "idx_department_manager", columnList = "manager_id")
        }
)
public class Department extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "manager_id")
    private Long managerId;
}
