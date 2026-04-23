package com.iwas.task.repository;

import com.iwas.task.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskIdAndIsDeletedFalseOrderByCreatedAtAsc(Long taskId);
}
