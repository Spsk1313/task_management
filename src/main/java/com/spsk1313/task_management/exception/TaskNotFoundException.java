package com.spsk1313.task_management.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long taskId) {
        super("Task with id " + taskId + " not found");
    }
}
