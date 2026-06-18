package com.iwas.task.repository;

import com.iwas.task.entity.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {

    List<TaskActivity> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
