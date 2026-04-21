package com.iwas.task.repository;

import com.iwas.task.entity.TaskSkillRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskSkillRequirementRepository extends JpaRepository<TaskSkillRequirement, Long> {

    List<TaskSkillRequirement> findByTaskId(Long taskId);

    Optional<TaskSkillRequirement> findByTaskIdAndSkillId(Long taskId, Long skillId);

    void deleteByTaskId(Long taskId);
}
