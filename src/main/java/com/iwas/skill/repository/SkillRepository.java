package com.iwas.skill.repository;

import com.iwas.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNameAndIsDeletedFalse(String name);

    @Query("SELECT s FROM Skill s WHERE s.isDeleted = false ORDER BY s.categoryId, s.name")
    List<Skill> findAllActive();

    @Query("SELECT s FROM Skill s WHERE s.isDeleted = false AND s.categoryId = :categoryId ORDER BY s.name")
    List<Skill> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            SELECT s FROM Skill s
            WHERE s.isDeleted = false
              AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR s.categoryId IN (
                       SELECT c.id FROM SkillCategory c
                       WHERE c.isDeleted = false
                         AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   ))
            ORDER BY s.categoryId, s.name
            """)
    List<Skill> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
            SELECT s FROM Skill s
            WHERE s.isDeleted = false
              AND s.categoryId = :categoryId
              AND (LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR s.categoryId IN (
                       SELECT c.id FROM SkillCategory c
                       WHERE c.isDeleted = false
                         AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   ))
            ORDER BY s.name
            """)
    List<Skill> searchByKeywordAndCategoryId(@Param("keyword") String keyword,
                                             @Param("categoryId") Long categoryId);
}
