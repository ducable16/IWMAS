package com.iwas.task.repository;

import com.iwas.task.entity.Task;
import com.iwas.task.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.projectId = :projectId ORDER BY t.dueDate, t.priority")
    List<Task> findByProjectId(Long projectId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assigneeId = :assigneeId ORDER BY t.dueDate, t.priority")
    List<Task> findByAssigneeId(Long assigneeId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assigneeId = :assigneeId AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findActiveTasksByAssigneeId(Long assigneeId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.projectId = :projectId AND t.status = :status")
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate < :date AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findOverdueTasks(LocalDate date);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate BETWEEN :from AND :to AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findTasksDueBetween(LocalDate from, LocalDate to);
}
