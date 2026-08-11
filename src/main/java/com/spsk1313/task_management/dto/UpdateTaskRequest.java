package com.spsk1313.task_management.dto;

import com.spsk1313.task_management.entity.TaskPriority;
import com.spsk1313.task_management.entity.TaskStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(
        @Size(max = 255)
        String title,

        String description,

        TaskPriority priority,

        TaskStatus status,

        LocalDate dueDate
) {
}