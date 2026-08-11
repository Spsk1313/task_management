package com.spsk1313.task_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTagRequest(
        @NotBlank
        @Size(max = 50)
        String name
) {
}
