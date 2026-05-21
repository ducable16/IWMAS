package com.iwas.task.repository;

import com.iwas.task.entity.Task;
import com.iwas.task.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.projectId = :projectId ORDER BY t.dueDate, t.priority")
    List<Task> findByProjectId(Long projectId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assigneeId = :assigneeId ORDER BY t.dueDate, t.priority")
    List<Task> findByAssigneeId(Long assigneeId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assigneeId = :assigneeId AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findActiveTasksByAssigneeId(Long assigneeId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.projectId = :projectId AND t.assigneeId = :assigneeId AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findActiveTasksByProjectIdAndAssigneeId(@Param("projectId") Long projectId, @Param("assigneeId") Long assigneeId);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.projectId = :projectId AND t.status = :status")
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate < :date AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findOverdueTasks(LocalDate date);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate BETWEEN :from AND :to AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findTasksDueBetween(LocalDate from, LocalDate to);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assigneeId = :assigneeId " +
           "AND t.dueDate BETWEEN :from AND :to AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findActiveTasksDueBetweenByAssignee(@Param("assigneeId") Long assigneeId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.assigneeId = :assigneeId " +
           "AND t.dueDate < :today AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findOverdueTasksByAssignee(@Param("assigneeId") Long assigneeId,
                                         @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate < :today " +
           "AND t.status NOT IN ('DONE', 'CANCELLED') AND t.assigneeId IS NOT NULL " +
           "AND (t.lastOverdueNotifiedAt IS NULL OR t.lastOverdueNotifiedAt < :today)")
    List<Task> findOverdueTasksNotNotifiedToday(@Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.isDeleted = false AND t.dueDate IN :dates " +
           "AND t.status NOT IN ('DONE', 'CANCELLED') AND t.assigneeId IS NOT NULL")
    List<Task> findTasksDueOn(@Param("dates") List<LocalDate> dates);
}
