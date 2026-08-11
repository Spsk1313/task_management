package com.spsk1313.task_management.dto;

import com.spsk1313.task_management.entity.TaskPriority;
import com.spsk1313.task_management.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Long projectId,
        TaskPriority priority,
        TaskStatus status,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        Set<String> tags
) {
}