package com.spsk1313.task_management.dto;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId
) {
}
