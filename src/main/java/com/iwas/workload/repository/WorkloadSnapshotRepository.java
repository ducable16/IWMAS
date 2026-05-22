package com.iwas.workload.repository;

import com.iwas.workload.entity.WorkloadSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkloadSnapshotRepository extends JpaRepository<WorkloadSnapshot, Long> {

    Optional<WorkloadSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate date);

    @Query("SELECT ws FROM WorkloadSnapshot ws WHERE ws.userId = :userId ORDER BY ws.snapshotDate DESC")
    List<WorkloadSnapshot> findByUserIdOrderByDateDesc(Long userId);

    @Query("SELECT ws FROM WorkloadSnapshot ws WHERE ws.snapshotDate = :date ORDER BY ws.overallPercent DESC")
    List<WorkloadSnapshot> findBySnapshotDate(LocalDate date);
}
