package com.spsk1313.task_management.dto;

import com.spsk1313.task_management.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        String description,

        @NotNull
        TaskPriority priority,

        LocalDate dueDate
) {
}