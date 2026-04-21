package com.iwas.timelog.repository;

import com.iwas.timelog.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    @Query("SELECT tl FROM TimeLog tl WHERE tl.isDeleted = false AND tl.taskId = :taskId ORDER BY tl.logDate DESC")
    List<TimeLog> findByTaskId(Long taskId);

    @Query("SELECT tl FROM TimeLog tl WHERE tl.isDeleted = false AND tl.userId = :userId AND tl.logDate BETWEEN :from AND :to ORDER BY tl.logDate DESC")
    List<TimeLog> findByUserIdAndDateRange(Long userId, LocalDate from, LocalDate to);

    Optional<TimeLog> findByTaskIdAndUserIdAndLogDateAndIsDeletedFalse(Long taskId, Long userId, LocalDate logDate);

    @Query("SELECT SUM(tl.hoursSpent) FROM TimeLog tl WHERE tl.isDeleted = false AND tl.taskId = :taskId")
    Double sumHoursByTaskId(Long taskId);
}
