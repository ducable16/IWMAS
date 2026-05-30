package com.iwas.task.repository;

import com.iwas.task.entity.TaskSkillRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskSkillRequirementRepository extends JpaRepository<TaskSkillRequirement, Long> {

    List<TaskSkillRequirement> findByTaskId(Long taskId);

    List<TaskSkillRequirement> findByTaskIdIn(Collection<Long> taskIds);

    Optional<TaskSkillRequirement> findByTaskIdAndSkillId(Long taskId, Long skillId);

    @Modifying
    @Query("DELETE FROM TaskSkillRequirement t WHERE t.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Query("""
            SELECT COUNT(tsr) FROM TaskSkillRequirement tsr
            WHERE tsr.skillId = :skillId
              AND tsr.taskId IN (
                  SELECT t.id FROM Task t
                  WHERE t.isDeleted = false
                    AND t.status NOT IN (com.iwas.task.enums.TaskStatus.DONE, com.iwas.task.enums.TaskStatus.CANCELLED)
              )
            """)
    long countOpenTaskRequirements(@Param("skillId") Long skillId);

    @Query("""
            SELECT COUNT(tsr) FROM TaskSkillRequirement tsr
            WHERE tsr.skillId = :skillId
              AND tsr.taskId IN (
                  SELECT t.id FROM Task t WHERE t.isDeleted = false
              )
            """)
    long countActiveTaskRequirements(@Param("skillId") Long skillId);
}
