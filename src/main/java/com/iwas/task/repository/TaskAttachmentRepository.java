package com.iwas.task.repository;

import com.iwas.task.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    @Query("SELECT a FROM TaskAttachment a WHERE a.taskId = :taskId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<TaskAttachment> findByTaskId(Long taskId);
}
