package com.spsk1313.task_management.repository;

import com.spsk1313.task_management.entity.Task;
import com.spsk1313.task_management.entity.TaskPriority;
import com.spsk1313.task_management.entity.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> belongsToProject(Long projectId) {
        return (root, query, cb) ->
                cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) ->
                status == null
                        ? null
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) ->
                priority == null
                        ? null
                        : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> searchByTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) {
                return null;
            }

            String pattern =
                    "%" + title.trim().toLowerCase(Locale.ROOT) + "%";

            return cb.like(
                    cb.lower(root.get("title")),
                    pattern
            );
        };
    }
}
