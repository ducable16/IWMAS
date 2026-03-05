package com.roamtrip.repository.issue;

import com.roamtrip.entity.issue.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    Optional<Issue> findByIdAndProject_Organization_Id(Long id, Long orgId);
}

