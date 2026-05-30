package com.iwas.skill.repository;

import com.iwas.skill.entity.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

    @Query("SELECT c FROM SkillCategory c WHERE c.isDeleted = false ORDER BY c.name")
    List<SkillCategory> findAllActive();

    @Query("SELECT c FROM SkillCategory c WHERE c.isDeleted = false AND LOWER(c.name) = LOWER(:name)")
    Optional<SkillCategory> findByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT COUNT(s) FROM Skill s WHERE s.isDeleted = false AND s.categoryId = :categoryId")
    long countActiveSkillsByCategory(@Param("categoryId") Long categoryId);
}
