package com.roamtrip.repository.project;

import com.roamtrip.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByIdAndOrganization_Id(Long id, Long orgId);
}

