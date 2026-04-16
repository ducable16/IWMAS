package com.roamtrip.skill.repository;

import com.roamtrip.skill.entity.EmployeeSkill;
import com.roamtrip.skill.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, Long> {

    @Query("SELECT es FROM EmployeeSkill es WHERE es.isDeleted = false AND es.userId = :userId")
    List<EmployeeSkill> findByUserId(Long userId);

    @Query("SELECT es FROM EmployeeSkill es WHERE es.isDeleted = false AND es.skillId = :skillId")
    List<EmployeeSkill> findBySkillId(Long skillId);

    Optional<EmployeeSkill> findByUserIdAndSkillIdAndIsDeletedFalse(Long userId, Long skillId);

    @Query("SELECT es FROM EmployeeSkill es WHERE es.isDeleted = false AND es.skillId = :skillId AND es.level IN :levels")
    List<EmployeeSkill> findBySkillIdAndLevelIn(Long skillId, List<SkillLevel> levels);
}
