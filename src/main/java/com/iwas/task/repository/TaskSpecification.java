package com.iwas.task.repository;

import com.iwas.task.dto.TaskFilterRequest;
import com.iwas.task.entity.Task;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskSpecification {

    private TaskSpecification() {}

    public static Specification<Task> fromFilter(TaskFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("projectId"), filter.getProjectId()));
            }

            if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.getStatuses()));
            }

            if (filter.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assigneeId"), filter.getAssigneeId()));
            }

            if (filter.getReporterId() != null) {
                predicates.add(cb.equal(root.get("reporterId"), filter.getReporterId()));
            }

            if (filter.getPriorities() != null && !filter.getPriorities().isEmpty()) {
                predicates.add(root.get("priority").in(filter.getPriorities()));
            }

            if (filter.getTypes() != null && !filter.getTypes().isEmpty()) {
                predicates.add(root.get("type").in(filter.getTypes()));
            }

            if (filter.getDueDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), filter.getDueDateFrom()));
            }

            if (filter.getDueDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), filter.getDueDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Task> byProjectId(Long projectId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.equal(root.get("projectId"), projectId)
        );
    }

    public static Specification<Task> unestimatedByProject(Long projectId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.equal(root.get("projectId"), projectId),
                root.get("estimatedHours").isNull()
        );
    }

    public static Specification<Task> unestimatedByProjects(List<Long> projectIds) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                root.get("projectId").in(projectIds),
                root.get("estimatedHours").isNull()
        );
    }

    public static Specification<Task> unassignedByProject(Long projectId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                cb.equal(root.get("projectId"), projectId),
                root.get("assigneeId").isNull()
        );
    }

    public static Specification<Task> unassignedByProjects(List<Long> projectIds) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("isDeleted"), false),
                root.get("projectId").in(projectIds),
                root.get("assigneeId").isNull()
        );
    }

    public static Specification<Task> forCalendar(LocalDate from, LocalDate to, Long projectId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("isDeleted"), false));
            predicates.add(root.get("dueDate").isNotNull());
            if (projectId != null) {
                predicates.add(cb.equal(root.get("projectId"), projectId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
