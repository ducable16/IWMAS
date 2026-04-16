package com.roamtrip.skill.repository;

import com.roamtrip.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    @Query("SELECT s FROM Skill s WHERE s.isDeleted = false ORDER BY s.category, s.name")
    List<Skill> findAllActive();

    Optional<Skill> findByNameAndIsDeletedFalse(String name);

    @Query("SELECT s FROM Skill s WHERE s.isDeleted = false AND s.category = :category ORDER BY s.name")
    List<Skill> findByCategory(String category);
}
